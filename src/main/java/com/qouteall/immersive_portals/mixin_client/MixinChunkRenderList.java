package com.qouteall.immersive_portals.mixin_client;

import com.qouteall.immersive_portals.ducks.IEChunkRenderList;
import net.minecraft.client.render.chunk.ChunkRenderer;
import net.minecraft.client.render.chunk.ChunkRendererList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(ChunkRendererList.class)
public class MixinChunkRenderList implements IEChunkRenderList {
    @Shadow
    private double cameraX;
    @Shadow
    private double cameraY;
    @Shadow
    private double cameraZ;
    
    @Mutable
    @Shadow
    @Final
    protected List<ChunkRenderer> chunkRenderers;
    
    @Override
    public void setCameraPos(double x, double y, double z) {
        cameraX = x;
        cameraY = y;
        cameraZ = z;
    }
    
    @Override
    public List<ChunkRenderer> getChunkRenderers() {
        return chunkRenderers;
    }
    
    @Override
    public void setChunkRenderers(List<ChunkRenderer> arg) {
        chunkRenderers = arg;
    }
}
