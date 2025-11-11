package qouteall.imm_ptl.core.portal.nether_portal;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.q_misc_util.my_util.MyTaskList;
import qouteall.q_misc_util.Helper;

import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class FrameSearching {
    // T is PortalGenInfo
    
    public static <T> void startSearchingPortalFrameAsync(
        WorldGenRegion region,
        int regionRadius,
        BlockPos centerPoint,
        Predicate<BlockState> framePredicate,
        Function<BlockPos.MutableBlockPos, T> matchShape,
        Consumer<T> onFound,
        Runnable onNotFound
    ) {
        Helper.log(String.format("FrameSearching: startSearching around %s radius %d", centerPoint, regionRadius));

        // Instead of doing heavy scanning in one tick or on a background thread that
        // accesses chunk internals (which causes deadlocks with concurrent chunk
        // engines like C2ME/dimthread), we perform an incremental search on the
        // server thread. Each server tick we process a small batch of chunks so
        // chunk loading can progress and we avoid long single-tick stalls.

        ArrayList<ChunkAccess> chunks = getChunksFromNearToFar(region, centerPoint, regionRadius);
        int minSectionY = McHelper.getMinSectionY(region);
        int yStart = McHelper.getMinY(region);
        int yEnd = McHelper.getMaxYExclusive(region);

        final int batchChunksPerTick = 4; // tuned small batch to avoid tick stalls

        // Fast path: if there are very few chunks, do the search immediately on
        // the server thread to avoid scheduling overhead and to handle same-chunk
        // cases quickly.
        if (chunks.size() <= batchChunksPerTick) {
            try {
                T result = searchPortalFrameWithYRange(
                    framePredicate, matchShape,
                    chunks, minSectionY,
                    yStart, yEnd
                );
                if (result != null) {
                    Helper.log("FrameSearching: fast-path found result");
                    onFound.accept(result);
                }
                else {
                    Helper.log("FrameSearching: fast-path not found");
                    onNotFound.run();
                }
            }
            catch (Throwable t) {
                Helper.logger.error("FrameSearching: exception during fast-path search", t);
                onNotFound.run();
            }

            return;
        }

        MyTaskList.MyTask task = new MyTaskList.MyTask() {
            int chunkIndex = 0;
            final BlockPos.MutableBlockPos tempPos = new BlockPos.MutableBlockPos();

            @Override
            public boolean runAndGetIsFinished() {
                try {
                    int processed = 0;
                    while (chunkIndex < chunks.size() && processed < batchChunksPerTick) {
                        ChunkAccess chunk = chunks.get(chunkIndex);
                        // search this chunk fully
                        LevelChunkSection[] sectionArray = chunk.getSections();

                        for (int ySectionIndex = 0; ySectionIndex < sectionArray.length; ySectionIndex++) {
                            int sectionY = ySectionIndex + minSectionY;

                            LevelChunkSection chunkSection = sectionArray[ySectionIndex];
                            if (chunkSection == null || chunkSection.hasOnlyAir()) {
                                continue;
                            }

                            int localYStart = Math.max(0, yStart - sectionY * 16);
                            int localYEnd = Math.min(16, yEnd - sectionY * 16);

                            for (int localY = localYStart; localY < localYEnd; localY++) {
                                for (int localZ = 0; localZ < 16; localZ++) {
                                    for (int localX = 0; localX < 16; localX++) {
                                        BlockState blockState = chunkSection.getBlockState(localX, localY, localZ);
                                        if (framePredicate.test(blockState)) {
                                            int worldX = localX + chunk.getPos().getMinBlockX();
                                            int worldY = localY + (sectionY) * 16;
                                            int worldZ = localZ + chunk.getPos().getMinBlockZ();
                                            tempPos.set(worldX, worldY, worldZ);

                                            T result = matchShape.apply(tempPos);
                                            if (result != null) {
                                                Helper.log(String.format("FrameSearching: found result at %s after scanning %d chunks", tempPos, chunkIndex + 1));
                                                onFound.accept(result);
                                                return true;
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        chunkIndex++;
                        processed++;
                    }

                    if (chunkIndex >= chunks.size()) {
                        // searched all chunks
                        Helper.log("FrameSearching: finished searching, not found");
                        onNotFound.run();
                        return true;
                    }

                    // not finished yet, continue next tick
                    return false;
                }
                catch (Throwable t) {
                    Helper.logger.error("FrameSearching: exception during incremental search", t);
                    onNotFound.run();
                    return true;
                }
            }

            @Override
            public void onCancelled() {
                // nothing special to do
            }
        };

        IPGlobal.serverTaskList.addTask(task);
    }

    // Return null for not found
    @Nullable
    public static <T> T searchPortalFrame(
        WorldGenRegion region,
        int regionRadius,
        BlockPos centerPoint,
        Predicate<BlockState> framePredicate,
        Function<BlockPos.MutableBlockPos, T> matchShape
    ) {
        ArrayList<ChunkAccess> chunks = getChunksFromNearToFar(
            region, centerPoint, regionRadius
        );

        int minSectionY = McHelper.getMinSectionY(region);
        int maxSectionYExclusive = McHelper.getMaxSectionYExclusive(region);

        return searchPortalFrameWithYRange(
            framePredicate, matchShape,
            chunks, minSectionY,
            McHelper.getMinY(region), McHelper.getMaxYExclusive(region)
        );
    }

    // After removing the usage of stream API, it becomes 100 times faster!!!
    @Nullable
    private static <T> T searchPortalFrameWithYRange(
        Predicate<BlockState> framePredicate,
        Function<BlockPos.MutableBlockPos, T> matchShape,
        ArrayList<ChunkAccess> chunks,
        int minSectionY,
        int yRangeStart, int yRangeEnd
    ) {
        BlockPos.MutableBlockPos temp = new BlockPos.MutableBlockPos();

        // avoid using stream api and maintain cache locality
        for (int chunkIndex = 0; chunkIndex < chunks.size(); chunkIndex++) {
            ChunkAccess chunk = chunks.get(chunkIndex);
            LevelChunkSection[] sectionArray = chunk.getSections();

            for (int ySectionIndex = 0; ySectionIndex < sectionArray.length; ySectionIndex++) {
                int sectionY = ySectionIndex + minSectionY;

                LevelChunkSection chunkSection = sectionArray[ySectionIndex];
                if (chunkSection != null && !chunkSection.hasOnlyAir()) {
                    int localYStart = Math.max(0, yRangeStart - sectionY * 16);
                    int localYEnd = Math.min(16, yRangeEnd - sectionY * 16);

                    for (int localY = localYStart; localY < localYEnd; localY++) {
                        for (int localZ = 0; localZ < 16; localZ++) {
                            for (int localX = 0; localX < 16; localX++) {
                                BlockState blockState = chunkSection.getBlockState(
                                    localX, localY, localZ
                                );
                                if (framePredicate.test(blockState)) {
                                    int worldX = localX + chunk.getPos().getMinBlockX();
                                    int worldY = localY + (sectionY) * 16;
                                    int worldZ = localZ + chunk.getPos().getMinBlockZ();
                                    temp.set(worldX, worldY, worldZ);

                                    T result = matchShape.apply(temp);
                                    if (result != null) {
                                        return result;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    private static ArrayList<ChunkAccess> getChunksFromNearToFar(
        WorldGenRegion region,
        BlockPos centerPoint,
        int regionRadius
    ) {
        ArrayList<ChunkAccess> chunks = new ArrayList<>();

        int searchedRadius = regionRadius - 1;
        int centerX = region.getCenter().x;
        int centerZ = region.getCenter().z;
        for (int x = centerX - searchedRadius; x <= centerX + searchedRadius; x++) {
            for (int z = centerZ - searchedRadius; z <= centerZ + searchedRadius; z++) {
                chunks.add(region.getChunk(x, z));
            }
        }

        chunks.sort(Comparator.comparingDouble(
            chunk -> chunk.getPos().getWorldPosition().distSqr(centerPoint)
        ));
        return chunks;
    }

    // Search using a provided chunk list on the server thread incrementally
    public static <T> void startSearchingPortalFrameAsyncFromChunks(
        ServerLevel world,
        ArrayList<ChunkAccess> chunks,
        BlockPos centerPoint,

        int regionRadius,
        Predicate<BlockState> framePredicate,
        Function<BlockPos.MutableBlockPos, T> matchShape,
        Consumer<T> onFound,
        Runnable onNotFound
    ) {
        Helper.log(String.format("FrameSearching: startSearchingFromChunks around %s radius %d with %d chunks", centerPoint, regionRadius, chunks.size()));

        int minSectionY = McHelper.getMinSectionY(world);
        int yStart = McHelper.getMinY(world);
        int yEnd = McHelper.getMaxYExclusive(world);

        final int batchChunksPerTick = 4;

        MyTaskList.MyTask task = new MyTaskList.MyTask() {
            int chunkIndex = 0;
            final BlockPos.MutableBlockPos tempPos = new BlockPos.MutableBlockPos();

            @Override
            public boolean runAndGetIsFinished() {
                try {
                    int processed = 0;
                    while (chunkIndex < chunks.size() && processed < batchChunksPerTick) {
                        ChunkAccess chunk = chunks.get(chunkIndex);
                        LevelChunkSection[] sectionArray = chunk.getSections();

                        for (int ySectionIndex = 0; ySectionIndex < sectionArray.length; ySectionIndex++) {
                            int sectionY = ySectionIndex + minSectionY;

                            LevelChunkSection chunkSection = sectionArray[ySectionIndex];
                            if (chunkSection == null || chunkSection.hasOnlyAir()) {
                                continue;
                            }

                            int localYStart = Math.max(0, yStart - sectionY * 16);
                            int localYEnd = Math.min(16, yEnd - sectionY * 16);

                            for (int localY = localYStart; localY < localYEnd; localY++) {
                                for (int localZ = 0; localZ < 16; localZ++) {
                                    for (int localX = 0; localX < 16; localX++) {
                                        BlockState blockState = chunkSection.getBlockState(localX, localY, localZ);
                                        if (framePredicate.test(blockState)) {
                                            int worldX = localX + chunk.getPos().getMinBlockX();
                                            int worldY = localY + (sectionY) * 16;
                                            int worldZ = localZ + chunk.getPos().getMinBlockZ();
                                            tempPos.set(worldX, worldY, worldZ);

                                            T result = matchShape.apply(tempPos);
                                            if (result != null) {
                                                Helper.log(String.format("FrameSearching: found result at %s after scanning %d chunks", tempPos, chunkIndex + 1));
                                                onFound.accept(result);
                                                return true;
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        chunkIndex++;
                        processed++;
                    }

                    if (chunkIndex >= chunks.size()) {
                        Helper.log("FrameSearching: finished searching from chunks, not found");
                        onNotFound.run();
                        return true;
                    }

                    return false;
                }
                catch (Throwable t) {
                    Helper.logger.error("FrameSearching: exception during incremental search from chunks", t);
                    onNotFound.run();
                    return true;
                }
            }

            @Override
            public void onCancelled() {}
        };

            IPGlobal.serverTaskList.addTask(task);
        }
    }
