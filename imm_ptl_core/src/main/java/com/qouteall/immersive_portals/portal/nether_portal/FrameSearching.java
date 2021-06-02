package com.qouteall.immersive_portals.portal.nether_portal;

import com.qouteall.immersive_portals.McHelper;
import net.minecraft.block.BlockState;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class FrameSearching {
    // T is PortalGenInfo
    
    public static <T> void startSearchingPortalFrameAsync(
        ChunkRegion region,
        int regionRadius,
        BlockPos centerPoint,
        Predicate<BlockState> framePredicate,
        Function<BlockPos.Mutable, T> matchShape,
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
                    McHelper.getServer().execute(() -> {
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
            Util.getMainWorkerExecutor()
        );
        
    }
    
    // Return null for not found
    // After removing the usage of stream API, it becomes 100 times faster!!!
    @Nullable
    public static <T> T searchPortalFrame(
        ChunkRegion region,
        int regionRadius,
        BlockPos centerPoint,
        Predicate<BlockState> framePredicate,
        Function<BlockPos.Mutable, T> matchShape
    ) {
        ArrayList<Chunk> chunks = getChunksFromNearToFar(
            region, centerPoint, regionRadius
        );
        
        BlockPos.Mutable temp = new BlockPos.Mutable();
        BlockPos.Mutable temp1 = new BlockPos.Mutable();
        
        // avoid using stream api and maintain cache locality
        for (int chunkIndex = 0; chunkIndex < chunks.size(); chunkIndex++) {
            Chunk chunk = chunks.get(chunkIndex);
            ChunkSection[] sectionArray = chunk.getSectionArray();
            for (int sectionY = 0; sectionY < sectionArray.length; sectionY++) {
                ChunkSection chunkSection = sectionArray[sectionY];
                if (chunkSection != null) {
                    for (int localY = 0; localY < 16; localY++) {
                        for (int localZ = 0; localZ < 16; localZ++) {
                            for (int localX = 0; localX < 16; localX++) {
                                BlockState blockState = chunkSection.getBlockState(
                                    localX, localY, localZ
                                );
                                if (framePredicate.test(blockState)) {
                                    int worldX = localX + chunk.getPos().getStartX();
                                    int worldY = localY + sectionY * 16;
                                    int worldZ = localZ + chunk.getPos().getStartZ();
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
    
    private static ArrayList<Chunk> getChunksFromNearToFar(
        ChunkRegion region,
        BlockPos centerPoint,
        int regionRadius
    ) {
        ArrayList<Chunk> chunks = new ArrayList<>();
        
        int searchedRadius = regionRadius - 1;
        int centerX = region.getCenterPos().x;
        int centerZ = region.getCenterPos().z;
        for (int x = centerX - searchedRadius; x <= centerX + searchedRadius; x++) {
            for (int z = centerZ - searchedRadius; z <= centerZ + searchedRadius; z++) {
                chunks.add(region.getChunk(x, z));
            }
        }
        
        chunks.sort(Comparator.comparingDouble(
            chunk -> chunk.getPos().getStartPos().getSquaredDistance(centerPoint)
        ));
        return chunks;
    }
}
