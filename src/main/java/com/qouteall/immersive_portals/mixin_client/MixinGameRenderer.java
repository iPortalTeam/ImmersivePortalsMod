package com.qouteall.immersive_portals.mixin_client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.SGlobal;
import com.qouteall.immersive_portals.exposer.IEGameRenderer;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer implements IEGameRenderer {
    @Shadow
    @Final
    @Mutable
    private LightmapTextureManager lightmapTextureManager;
    @Shadow
    @Final
    @Mutable
    private BackgroundRenderer backgroundRenderer;
    @Shadow
    private boolean renderHand;
    @Shadow
    @Final
    @Mutable
    private Camera camera;
    
    @Shadow
    public abstract void renderCenter(float float_1, long long_1);
    
    @Override
    public void renderCenter_(float partialTicks, long finishTimeNano) {
        renderCenter(partialTicks, finishTimeNano);
    }
    
    @Inject(
        method = "Lnet/minecraft/client/render/GameRenderer;renderCenter(FJ)V",
        at = @At(
            value = "INVOKE",
            shift = At.Shift.AFTER,
            target = "Lnet/minecraft/client/render/WorldRenderer;renderEntities(Lnet/minecraft/client/render/Camera;Lnet/minecraft/client/render/VisibleRegion;F)V"
        )
    )
    private void afterRenderingEntities(
        float partialTicks,
        long finishTimeNano,
        CallbackInfo ci
    ) {
        if (SGlobal.renderPortalBeforeTranslucentBlocks) {
            CGlobal.renderer.doRendering(partialTicks, finishTimeNano);
        }
    }
    
    @Inject(
        method = "renderCenter",
        at = @At(
            value = "INVOKE_STRING",
            target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V",
            args = {"ldc=hand"}
        )
    )
    private void beforeRenderingHand(float float_1, long long_1, CallbackInfo ci) {
        if (!SGlobal.renderPortalBeforeTranslucentBlocks) {
            CGlobal.renderer.doRendering(float_1, long_1);
        }
    }
    
    @Redirect(
        method = "Lnet/minecraft/client/render/GameRenderer;renderCenter(FJ)V",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/platform/GlStateManager;clear(IZ)V"
        )
    )
    private void redirectClearing(int int_1, boolean boolean_1) {
        if (!CGlobal.renderer.shouldSkipClearing()) {
            GlStateManager.clear(int_1, boolean_1);
        }
    }
    
    @Inject(
        method = "Lnet/minecraft/client/render/GameRenderer;render(FJZ)V",
        at = @At("HEAD")
    )
    private void onPreRender(float float_1, long long_1, boolean boolean_1, CallbackInfo ci) {
        ModMain.preRenderSignal.emit();
    }
    
    @Shadow
    abstract protected void applyCameraTransformations(float float_1);
    
    
    @Override
    public void applyCameraTransformations_(float float_1) {
        applyCameraTransformations(float_1);
    }
    
    @Override
    public LightmapTextureManager getLightmapTextureManager() {
        return lightmapTextureManager;
    }
    
    @Override
    public void setLightmapTextureManager(LightmapTextureManager manager) {
        lightmapTextureManager = manager;
    }
    
    @Override
    public BackgroundRenderer getBackgroundRenderer() {
        return backgroundRenderer;
    }
    
    @Override
    public void setBackgroundRenderer(BackgroundRenderer renderer) {
        backgroundRenderer = renderer;
    }
    
    @Override
    public boolean getDoRenderHand() {
        return renderHand;
    }
    
    @Override
    public void setDoRenderHand(boolean e) {
        renderHand = e;
    }
    
    @Override
    public void setCamera(Camera camera_) {
        camera = camera_;
    }
}
