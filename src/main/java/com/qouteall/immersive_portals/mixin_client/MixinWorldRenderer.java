package com.qouteall.immersive_portals.mixin_client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.CHelper;
import com.qouteall.immersive_portals.ClientWorldLoader;
import com.qouteall.immersive_portals.exposer.IEWorldRenderer;
import com.qouteall.immersive_portals.exposer.IEWorldRendererChunkInfo;
import net.minecraft.block.BlockRenderLayer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.chunk.ChunkRenderer;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(WorldRenderer.class)
public abstract class MixinWorldRenderer implements IEWorldRenderer {
    
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
    
    @Shadow
    @Final
    public MinecraftClient client;
    
    @Shadow
    private double lastTranslucentSortX;
    
    @Shadow
    private double lastTranslucentSortY;
    
    @Shadow
    private double lastTranslucentSortZ;
    
    @Shadow
    protected abstract void renderLayer(BlockRenderLayer blockLayerIn);
    
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
        if (CGlobal.renderer.isRendering()) {
            CGlobal.myGameRenderer.startCulling();
        }
    }
    
    @Inject(
        method = "Lnet/minecraft/client/render/WorldRenderer;renderLayer(Lnet/minecraft/block/BlockRenderLayer;)V",
        at = @At("TAIL")
    )
    private void onStopRenderLayer(BlockRenderLayer blockRenderLayer_1, CallbackInfo ci) {
        if (CGlobal.renderer.isRendering()) {
            CGlobal.myGameRenderer.endCulling();
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
        CGlobal.myGameRenderer.renderPlayerItselfIfNecessary();
    }
    
    private static boolean isReloadingOtherWorldRenderers = false;
    
    //reload other world renderers when the main world renderer is reloaded
    @Inject(method = "reload", at = @At("TAIL"))
    private void onReload(CallbackInfo ci) {
        ClientWorldLoader clientWorldLoader = CGlobal.clientWorldLoader;
        WorldRenderer this_ = (WorldRenderer) (Object) this;
        if (isReloadingOtherWorldRenderers) {
            return;
        }
        if (CGlobal.renderer.isRendering()) {
            return;
        }
        if (clientWorldLoader.getIsLoadingFakedWorld()) {
            return;
        }
        if (this_ != MinecraftClient.getInstance().worldRenderer) {
            return;
        }
        
        isReloadingOtherWorldRenderers = true;
        
        for (WorldRenderer worldRenderer : clientWorldLoader.worldRendererMap.values()) {
            if (worldRenderer != this_) {
                worldRenderer.reload();
            }
        }
        isReloadingOtherWorldRenderers = false;
    }
    
    //avoid resort transparency when rendering portal
    @Inject(
        method = "renderLayer(Lnet/minecraft/block/BlockRenderLayer;Lnet/minecraft/client/render/Camera;)I",
        at = @At("HEAD"),
        cancellable = true
    )
    public void onRenderLayer(
        BlockRenderLayer blockLayerIn,
        Camera activeRenderInfo,
        CallbackInfoReturnable<Integer> cir
    ) {
        if (blockLayerIn == BlockRenderLayer.TRANSLUCENT && CGlobal.renderer.isRendering()) {
            //run my version and avoid resort transparency
            
            GuiLighting.disable();
            
            this.client.getProfiler().push("filterempty");
            int l = 0;
            boolean flag = blockLayerIn == BlockRenderLayer.TRANSLUCENT;
            int i1 = flag ? this.chunkInfos.size() - 1 : 0;
            int i = flag ? -1 : this.chunkInfos.size();
            int j1 = flag ? -1 : 1;
            
            for (int j = i1; j != i; j += j1) {
                ChunkRenderer chunkrender = ((IEWorldRendererChunkInfo) this.chunkInfos.get(j)).getChunkRenderer();
                if (!chunkrender.getData().isEmpty(blockLayerIn)) {
                    ++l;
                    this.chunkRendererList.add(chunkrender, blockLayerIn);
                }
            }
            
            if (l == 0) {
                this.client.getProfiler().pop();
            }
            else {
                if (CHelper.shouldDisableFog()) {
                    GlStateManager.disableFog();
                }
                
                this.client.getProfiler().swap(() -> {
                    return "render_" + blockLayerIn;
                });
                this.renderLayer(blockLayerIn);
                this.client.getProfiler().pop();
            }
            
            cir.setReturnValue(l);
            cir.cancel();
        }
    }
    
    @Override
    public EntityRenderDispatcher getEntityRenderDispatcher() {
        return entityRenderDispatcher;
    }
}
