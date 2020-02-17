package com.qouteall.immersive_portals.far_scenery;

import com.google.common.collect.Queues;
import com.qouteall.immersive_portals.render.MyBuiltChunkStorage;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.function.Predicate;

public class SectionRenderListPropagator {
    public static List<ChunkBuilder.BuiltChunk> getRenderSectionList(
        MyBuiltChunkStorage chunks,
        BlockPos cameraPos,
        int renderDistance,
        Predicate<ChunkBuilder.BuiltChunk> isInFrustum,
        int uniqueInt
    ) {
        List<ChunkBuilder.BuiltChunk> result = new ArrayList<>();
        
        ChunkBuilder.BuiltChunk starting = chunks.myGetRenderChunkRaw(cameraPos, chunks.chunks);
        
        Predicate<ChunkBuilder.BuiltChunk> visitAndGetIsNewlyVisiting =
            builtChunk -> builtChunk.setRebuildFrame(uniqueInt);
        
        visitAndGetIsNewlyVisiting.test(starting);
        
        Queue<ChunkBuilder.BuiltChunk> queue = Queues.newArrayDeque();
        queue.add(starting);
        
        while (!queue.isEmpty()) {
            ChunkBuilder.BuiltChunk curr = queue.poll();
            result.add(curr);
            
            for (Direction direction : Direction.values()) {
                ChunkBuilder.BuiltChunk adjacentChunk = getAdjacentChunk(
                    cameraPos, curr, direction, renderDistance, chunks
                );
                
                if (adjacentChunk != null) {
                    if (visitAndGetIsNewlyVisiting.test(adjacentChunk)) {
                        if (isInFrustum.test(curr)) {
                            queue.add(adjacentChunk);
                        }
                    }
                }
            }
        }
        
        return result;
    }
    
    private static ChunkBuilder.BuiltChunk getAdjacentChunk(
        BlockPos cameraPos,
        ChunkBuilder.BuiltChunk chunk,
        Direction direction,
        int renderDistance,
        MyBuiltChunkStorage chunks
    ) {
        BlockPos neighborPos = chunk.getNeighborPosition(direction);
        if (MathHelper.abs(cameraPos.getX() - neighborPos.getX()) > renderDistance * 16) {
            return null;
        }
        else if (neighborPos.getY() >= 0 && neighborPos.getY() < 256) {
            if (MathHelper.abs(cameraPos.getZ() - neighborPos.getZ()) > renderDistance * 16) {
                return null;
            }
            else {
                return chunks.myGetRenderChunkRaw(neighborPos, chunks.chunks);
            }
        }
        else {
            return null;
        }
    }
}
