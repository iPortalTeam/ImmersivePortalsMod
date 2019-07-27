package com.qouteall.immersive_portals.render;

import com.qouteall.immersive_portals.exposer.IEBackgroundRenderer;
import com.qouteall.immersive_portals.exposer.IEGameRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class DimensionRenderHelper {
    private MinecraftClient mc;
    public World world;
    
    public final BackgroundRenderer fogRenderer;
    
    public final LightmapTextureManager lightmapTexture;
    
    public DimensionRenderHelper(World world) {
        mc = MinecraftClient.getInstance();
        this.world = world;
    
        if (mc.world == world) {
            IEGameRenderer gameRenderer = (IEGameRenderer) mc.gameRenderer;
        
            lightmapTexture = gameRenderer.getLightmapTextureManager();
            fogRenderer = gameRenderer.getBackgroundRenderer();
        }
        else {
            lightmapTexture = new LightmapTextureManager(mc.gameRenderer);
            fogRenderer = new BackgroundRenderer(mc.gameRenderer);
        }
    
        ((IEBackgroundRenderer) fogRenderer).setDimensionConstraint(world.dimension.getType());
    }
    
    public Vec3d getFogColor() {
        return ((IEBackgroundRenderer) fogRenderer).getFogColor();
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
    
    public void switchToMe() {
        IEGameRenderer gameRenderer = (IEGameRenderer) mc.gameRenderer;
        gameRenderer.setBackgroundRenderer(fogRenderer);
        gameRenderer.setLightmapTextureManager(lightmapTexture);
    }
}
