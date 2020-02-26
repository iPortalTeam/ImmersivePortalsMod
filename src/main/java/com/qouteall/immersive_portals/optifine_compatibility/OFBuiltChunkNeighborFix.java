package com.qouteall.immersive_portals.optifine_compatibility;

import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.OFInterface;
import com.qouteall.immersive_portals.render.MyBuiltChunkStorage;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.lang.reflect.Method;

public class OFBuiltChunkNeighborFix {
    private static Method method_setRenderChunkNeighbour;
    
    public static void init() {
        method_setRenderChunkNeighbour = Helper.noError(() ->
            ChunkBuilder.BuiltChunk.class
                .getDeclaredMethod(
                    "setRenderChunkNeighbour",
                    Direction.class,
                    ChunkBuilder.BuiltChunk.class
                )
        );
    }
    
    public static void updateNeighbor(
        MyBuiltChunkStorage storage,
        ChunkBuilder.BuiltChunk[] chunks
    ) {
        if (!OFInterface.isOptifinePresent) {
            return;
        }
        
        MinecraftClient.getInstance().getProfiler().push("neighbor");
        
        try {
            for (int l = 0; l < Direction.values().length; ++l) {
                Direction facing = Direction.values()[l];
                for (int i = 0, chunksLength = chunks.length; i < chunksLength; i++) {
                    ChunkBuilder.BuiltChunk renderChunk = chunks[i];
                    BlockPos neighborPos = renderChunk.getNeighborPosition(facing);
                    ChunkBuilder.BuiltChunk neighbour =
                        storage.myGetRenderChunkRaw(neighborPos, chunks);
                    method_setRenderChunkNeighbour.invoke(
                        renderChunk, facing, neighbour
                    );
                }
            }
        }
        catch (Throwable e) {
            throw new IllegalStateException(e);
        }
        
        MinecraftClient.getInstance().getProfiler().pop();
    }
}
