package com.qouteall.immersive_portals.render;

import com.qouteall.immersive_portals.exposer.IEBackgroundRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class DimensionRenderHelper {
    private MinecraftClient mc;
    private World world;
    
    public final BackgroundRenderer fogRenderer;
    
    public final LightmapTextureManager lightmapTexture;
    
    public DimensionRenderHelper(World world) {
        mc = MinecraftClient.getInstance();
        this.world = world;
        
        lightmapTexture = new LightmapTextureManager(mc.gameRenderer);
        
        fogRenderer = new BackgroundRenderer(mc.gameRenderer);
    }
    
    //copied from updateFogColor()
    public Vec3d getFogColor() {
        return ((IEBackgroundRenderer) fogRenderer).getFogColor();
    }
    
    public void tick() {
        lightmapTexture.tick();
    }
}
