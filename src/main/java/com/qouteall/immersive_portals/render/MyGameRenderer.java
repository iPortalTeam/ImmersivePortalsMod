package com.qouteall.immersive_portals.render;

import com.qouteall.immersive_portals.Globals;
import com.qouteall.immersive_portals.exposer.IEGameRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.SystemUtil;

public class MyGameRenderer {
    private MinecraftClient mc = MinecraftClient.getInstance();
    
    public void renderWorld(
        float partialTicks,
        WorldRenderer newWorldRenderer,
        ClientWorld newWorld
    ) {
        IEGameRenderer ieGameRenderer = (IEGameRenderer) mc.gameRenderer;
        DimensionRenderHelper dimensionRenderHelper =
            Globals.clientWorldLoader.getDimensionRenderHelper(newWorld.dimension.getType());
        
        WorldRenderer oldWorldRenderer = mc.worldRenderer;
        ClientWorld oldWorld = mc.world;
        LightmapTextureManager oldLightmap = ieGameRenderer.getLightmapTextureManager();
        BackgroundRenderer oldFogRenderer = ieGameRenderer.getBackgroundRenderer();
        assert BlockEntityRenderDispatcher.INSTANCE.world == oldWorld;
        
        mc.worldRenderer = newWorldRenderer;
        mc.world = newWorld;
        ieGameRenderer.setBackgroundRenderer(dimensionRenderHelper.fogRenderer);
        ieGameRenderer.setLightmapTextureManager(dimensionRenderHelper.lightmapTexture);
        BlockEntityRenderDispatcher.INSTANCE.world = newWorld;
        
        mc.getProfiler().push("render_portal_content");
        
        //invoke it!
        mc.gameRenderer.renderWorld(partialTicks, getChunkUpdateFinishTime());
        
        mc.getProfiler().pop();
        
        mc.worldRenderer = oldWorldRenderer;
        mc.world = oldWorld;
        ieGameRenderer.setBackgroundRenderer(oldFogRenderer);
        ieGameRenderer.setLightmapTextureManager(oldLightmap);
        BlockEntityRenderDispatcher.INSTANCE.world = oldWorld;
    }
    
    private long getChunkUpdateFinishTime() {
        int idealFps = Math.min(MinecraftClient.getCurrentFps(), this.mc.options.maxFps);
        idealFps = Math.max(idealFps, 60);
        long idealPassTime = (1000000000 / idealFps / 4);
        return SystemUtil.getMeasuringTimeNano() + Math.max(idealPassTime, 0L);
    }
}
