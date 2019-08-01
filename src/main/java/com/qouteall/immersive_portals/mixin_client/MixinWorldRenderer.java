package com.qouteall.immersive_portals.mixin_client;

import com.qouteall.immersive_portals.Globals;
import com.qouteall.immersive_portals.exposer.IEWorldRenderer;
import net.minecraft.block.BlockRenderLayer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.ChunkRenderDispatcher;
import net.minecraft.client.render.VisibleRegion;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.chunk.ChunkRendererFactory;
import net.minecraft.client.render.chunk.ChunkRendererList;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Final;
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
    
    @Shadow
    @Final
    private EntityRenderDispatcher entityRenderDispatcher;
    
    @Shadow
    private int field_4076;
    
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
    
    @Inject(
        method = "renderEntities",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/GameRenderer;enableLightmap()V",
            shift = At.Shift.AFTER
        )
    )
    private void onEndRenderEntities(
        Camera camera_1,
        VisibleRegion visibleRegion_1,
        float float_1,
        CallbackInfo ci
    ) {
        Globals.myGameRenderer.renderPlayerItselfIfNecessary();
    }
    
    @Override
    public EntityRenderDispatcher getEntityRenderDispatcher() {
        return entityRenderDispatcher;
    }
}
