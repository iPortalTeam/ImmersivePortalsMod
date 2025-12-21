package qouteall.imm_ptl.core.portal.nether_portal;

import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.jetbrains.annotations.Nullable;
import qouteall.imm_ptl.core.McHelper;
import qouteall.q_misc_util.MiscHelper;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class FrameSearching {
    
    @FunctionalInterface
    public static interface FrameSearchingFunc<T> {
        T searchAt(FastBlockAccess blockAccess, int x, int y, int z);
    }
    
    
    // T is PortalGenInfo
    
    public static <T> void startSearchingPortalFrameAsync(
        FastBlockAccess region,
        int regionRadius,
        BlockPos centerPoint,
        Predicate<BlockState> framePredicate,
        FrameSearchingFunc<T> matchShape,
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
        FastBlockAccess region,
        int regionRadius,
        BlockPos centerPoint,
        Predicate<BlockState> framePredicate,
        FrameSearchingFunc<T> matchShape
    ) {
        List<ChunkPos> chunks = getChunksFromNearToFar(
            region, centerPoint, regionRadius
        );
        
        int minSectionY = region.minSectionY();
        int maxSectionYExclusive = region.maxSectionYExclusive();
        
        return searchPortalFrameWithYRange(
            region,
            framePredicate, matchShape,
            chunks, minSectionY,
            McHelper.getMinY(region.world()), McHelper.getMaxYExclusive(region.world())
        );
    }
    
    // After removing the usage of stream API, it becomes 100 times faster!!!
    @Nullable
    private static <T> T searchPortalFrameWithYRange(
        FastBlockAccess fastBlockAccess,
        Predicate<BlockState> framePredicate,
        FrameSearchingFunc<T> matchShape,
        List<ChunkPos> chunkPoses,
        int minSectionY,
        int yRangeStart, int yRangeEnd
    ) {
        // avoid using stream api and maintain cache locality
        for (ChunkPos chunkPos : chunkPoses) {
            for (
                int cy = fastBlockAccess.minSectionY();
                cy < fastBlockAccess.maxSectionYExclusive();
                cy++
            ) {
                LevelChunkSection chunkSection = fastBlockAccess.getSection(
                    chunkPos.x, cy, chunkPos.z
                );
                if (chunkSection != null && !chunkSection.hasOnlyAir()) {
                    int localYStart = Math.max(0, yRangeStart - cy * 16);
                    int localYEnd = Math.min(16, yRangeEnd - cy * 16);
                    
                    for (int localY = localYStart; localY < localYEnd; localY++) {
                        for (int localZ = 0; localZ < 16; localZ++) {
                            for (int localX = 0; localX < 16; localX++) {
                                BlockState blockState = chunkSection.getBlockState(
                                    localX, localY, localZ
                                );
                                if (framePredicate.test(blockState)) {
                                    int worldX = localX + chunkPos.getMinBlockX();
                                    int worldY = localY + cy * 16;
                                    int worldZ = localZ + chunkPos.getMinBlockZ();
                                    
                                    T result = matchShape.searchAt(
                                        fastBlockAccess, worldX, worldY, worldZ
                                    );
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
    
    private static List<ChunkPos> getChunksFromNearToFar(
        FastBlockAccess region,
        BlockPos centerPoint,
        int regionRadius
    ) {
        
        return region.chunkPoses()
            .sorted(Comparator.comparingDouble(
                chunk -> chunk.getWorldPosition().distSqr(centerPoint)
            ))
            .collect(Collectors.toList());
    }
}
