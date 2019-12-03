package com.qouteall.immersive_portals.mixin_client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.ModMainClient;
import com.qouteall.immersive_portals.ducks.IEGameRenderer;
import com.qouteall.immersive_portals.render.RenderHelper;
import net.minecraft.client.MinecraftClient;
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
        if (MinecraftClient.getInstance().cameraEntity != null) {
            CGlobal.renderer.onBeforeTranslucentRendering();
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
        if (MinecraftClient.getInstance().cameraEntity != null) {
            CGlobal.renderer.onAfterTranslucentRendering();
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
    
    //may do teleportation here
    @Inject(method = "render", at = @At("HEAD"))
    private void onFarBeforeRendering(
        float partialTicks,
        long nanoTime,
        boolean renderWorldIn,
        CallbackInfo ci
    ) {
        RenderHelper.updatePreRenderInfo(partialTicks);
        ModMain.preRenderSignal.emit();
    }
    
    //before rendering world (not triggered when rendering portal)
    @Inject(
        method = "renderWorld",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/GameRenderer;renderCenter(FJ)V"
        )
    )
    private void onBeforeRenderingCenter(float partialTicks, long finishTimeNano, CallbackInfo ci) {
        ModMainClient.switchToCorrectRenderer();
    
        CGlobal.renderer.prepareRendering();
    }
    
    //after rendering world (not triggered when rendering portal)
    @Inject(
        method = "renderWorld",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/GameRenderer;renderCenter(FJ)V",
            shift = At.Shift.AFTER
        )
    )
    private void onAfterRenderingCenter(float partialTicks, long finishTimeNano, CallbackInfo ci) {
        CGlobal.renderer.finishRendering();
    
        RenderHelper.onTotalRenderEnd();
    }
    
    @Inject(method = "renderCenter", at = @At("TAIL"))
    private void onRenderCenterEnded(float partialTicks, long nanoTime, CallbackInfo ci) {
        CGlobal.renderer.onRenderCenterEnded();
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
