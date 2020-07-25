package com.qouteall.hiding_in_the_bushes.sodium_compatibility;

import com.qouteall.hiding_in_the_bushes.SodiumInterface;
import me.jellysquid.mods.sodium.client.IWorldRenderer;
import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderBackend;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderManager;

public class SodiumInterfaceInitializer {
    public static void init() {
        SodiumInterface.createClientChunkManager = ClientChunkManagerWithSodium::new;
        
        SodiumInterface.createNewRenderingContext = worldRenderer -> {
            SodiumWorldRenderer swr = ((IWorldRenderer) worldRenderer).getSodiumWorldRenderer();
            ChunkRenderBackend<?> backend = swr.getChunkRenderer();
            int passCount = backend.getRenderPassManager().getPassCount();
            return new ChunkRenderManager.RenderContext<>(passCount);
        };
        
        SodiumInterface.switchRenderingContext = (worldRenderer, newContext) -> {
            SodiumWorldRenderer sodiumWorldRenderer =
                ((IWorldRenderer) worldRenderer).getSodiumWorldRenderer();
            
            // must update render list
            sodiumWorldRenderer.scheduleTerrainUpdate();
            
            return sodiumWorldRenderer.switchRenderContext(((ChunkRenderManager.RenderContext) newContext));
        };
    }
}
