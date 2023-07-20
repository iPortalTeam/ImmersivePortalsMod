package qouteall.imm_ptl.core.portal.nether_portal;

import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import qouteall.imm_ptl.core.McHelper;
import qouteall.q_misc_util.MiscHelper;

import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.CompletableFuture;
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
        CompletableFuture<Void> future = CompletableFuture.runAsync(
            () -> {
                try {
                    T result = searchPortalFrame(
                        region, regionRadius,
                        centerPoint, framePredicate,
                        matchShape
                    );
                    MiscHelper.getServer().execute(() -> {
                        if (result != null) {
                            onFound.accept(result);
                        }
                        else {
                            onNotFound.run();
                        }
                    });
                }
                catch (Throwable oops) {
                    oops.printStackTrace();
                    onNotFound.run();
                }
            },
            Util.backgroundExecutor()
        );
        
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
}
