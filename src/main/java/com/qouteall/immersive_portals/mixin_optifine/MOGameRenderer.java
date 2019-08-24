package com.qouteall.immersive_portals.mixin_optifine;

import com.qouteall.immersive_portals.CGlobal;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class MOGameRenderer {
    @Inject(
        method = "renderCenter",
        at = @At(
            value = "INVOKE",
            target = "Lnet/optifine/shaders/Shaders;endRender()V",
            shift = At.Shift.AFTER
        )
    )
    private void onShaderRenderEnded(float partialTicks, long nanoTime, CallbackInfo ci) {
        CGlobal.renderer.onShaderRenderEnded();
    }
}
