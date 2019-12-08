package com.qouteall.immersive_portals.render;

import com.qouteall.immersive_portals.ducks.IEGameRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.world.World;

public class DimensionRenderHelper {
    private MinecraftClient mc;
    public World world;
    
    public final LightmapTextureManager lightmapTexture;
    
    public DimensionRenderHelper(World world) {
        mc = MinecraftClient.getInstance();
        this.world = world;
    
        if (mc.world == world) {
            IEGameRenderer gameRenderer = (IEGameRenderer) mc.gameRenderer;
        
            lightmapTexture = gameRenderer.getLightmapTextureManager();
        }
        else {
            lightmapTexture = new LightmapTextureManager(mc.gameRenderer, mc);
        }
    }
    
    public void tick() {
        lightmapTexture.tick();
    }
    
    //TODO cleanup it
    public void cleanUp() {
        if (world != mc.world) {
            lightmapTexture.close();
        }
    }
    
}
