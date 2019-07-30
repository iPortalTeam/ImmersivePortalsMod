package com.qouteall.immersive_portals.mixin;

import com.qouteall.immersive_portals.Globals;
import com.qouteall.immersive_portals.exposer.IEWorldRenderer;
import net.minecraft.block.BlockRenderLayer;
import net.minecraft.client.render.ChunkRenderDispatcher;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.chunk.ChunkRendererFactory;
import net.minecraft.client.render.chunk.ChunkRendererList;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(WorldRenderer.class)
public class MixinWorldRenderer implements IEWorldRenderer {
    @Shadow
    private ClientWorld world;
    
    @Shadow
    private ChunkRendererFactory chunkRendererFactory;
    
    @Shadow
    private ChunkRenderDispatcher chunkRenderDispatcher;
    
    @Shadow
    private ChunkRendererList chunkRendererList;
    
    @Shadow
    private List chunkInfos;
    
    @Override
    public ChunkRenderDispatcher getChunkRenderDispatcher() {
        return chunkRenderDispatcher;
    }
    
    @Override
    public ChunkRendererList getChunkRenderList() {
        return chunkRendererList;
    }
    
    @Override
    public List getChunkInfos() {
        return chunkInfos;
    }
    
    @Override
    public void setChunkInfos(List list) {
        chunkInfos = list;
    }
    
    @Inject(
        method = "Lnet/minecraft/client/render/WorldRenderer;renderLayer(Lnet/minecraft/block/BlockRenderLayer;)V",
        at = @At("HEAD")
    )
    private void onStartRenderLayer(BlockRenderLayer blockRenderLayer_1, CallbackInfo ci) {
        if (Globals.portalRenderManager.isRendering()) {
            Globals.myGameRenderer.startCulling();
        }
    }
    
    @Inject(
        method = "Lnet/minecraft/client/render/WorldRenderer;renderLayer(Lnet/minecraft/block/BlockRenderLayer;)V",
        at = @At("TAIL")
    )
    private void onStopRenderLayer(BlockRenderLayer blockRenderLayer_1, CallbackInfo ci) {
        if (Globals.portalRenderManager.isRendering()) {
            Globals.myGameRenderer.endCulling();
        }
    }
}
