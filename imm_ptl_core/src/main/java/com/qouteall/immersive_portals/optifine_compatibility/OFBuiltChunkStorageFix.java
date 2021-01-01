package com.qouteall.immersive_portals.optifine_compatibility;

import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.OFInterface;
import com.qouteall.immersive_portals.render.MyBuiltChunkStorage;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BuiltChunkStorage;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.lang.reflect.Method;

public class OFBuiltChunkStorageFix {
    private static Method BuiltChunk_setRenderChunkNeighbour;
    
    private static Method BuiltChunkStorage_updateVboRegion;
    
    private static Method VboRegion_deleteGlBuffers;
    
    public static void init() {
        BuiltChunk_setRenderChunkNeighbour = Helper.noError(() ->
            ChunkBuilder.BuiltChunk.class
                .getDeclaredMethod(
                    "setRenderChunkNeighbour",
                    Direction.class,
                    ChunkBuilder.BuiltChunk.class
                )
        );
        BuiltChunkStorage_updateVboRegion = Helper.noError(() ->
            BuiltChunkStorage.class
                .getDeclaredMethod(
                    "updateVboRegion",
                    ChunkBuilder.BuiltChunk.class
                )
        );
        BuiltChunkStorage_updateVboRegion.setAccessible(true);
    }
    
    public static void onBuiltChunkCreated(
        BuiltChunkStorage builtChunkStorage,
        ChunkBuilder.BuiltChunk builtChunk
    ) {
        if (!OFInterface.isOptifinePresent) {
            return;
        }
        
        Helper.noError(() ->
            BuiltChunkStorage_updateVboRegion.invoke(builtChunkStorage, builtChunk)
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
                for (ChunkBuilder.BuiltChunk renderChunk : chunks) {
                    BlockPos neighborPos = renderChunk.getNeighborPosition(facing);
                    ChunkBuilder.BuiltChunk neighbour =
                        storage.myGetRenderChunkRaw(neighborPos, chunks);
                    BuiltChunk_setRenderChunkNeighbour.invoke(
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
