package com.qouteall.immersive_portals.mixin;

import com.qouteall.immersive_portals.exposer.IEChunkRenderList;
import net.minecraft.client.render.chunk.ChunkRendererList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ChunkRendererList.class)
public class MixinChunkRenderList implements IEChunkRenderList {
    @Shadow
    private double cameraX;
    @Shadow
    private double cameraY;
    @Shadow
    private double cameraZ;
    
    @Override
    public void setCameraPos(double x, double y, double z) {
        cameraX = x;
        cameraY = y;
        cameraZ = z;
    }
}
