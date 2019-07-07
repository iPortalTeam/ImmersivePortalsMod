package com.qouteall.immersive_portals.mixin;

import com.qouteall.immersive_portals.Globals;
import com.qouteall.immersive_portals.exposer.IEGameRenderer;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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
    
    @Inject(
        method = "Lnet/minecraft/client/render/GameRenderer;renderCenter(FJ)V",
        at = @At(value = "INVOKE_STRING", target = "translucent")
    )
    private void beforeRenderingTranslucent(
        float partialTicks,
        long finishTimeNano,
        CallbackInfo ci
    ) {
        Globals.portalRenderManager.doRendering(partialTicks, finishTimeNano);
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
}
