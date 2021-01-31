package com.qouteall.immersive_portals.mixin.client.debug.isometric;

import com.qouteall.immersive_portals.render.TransformationManager;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.util.math.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class MixinGameRenderer_I {
    @Inject(
        method = "getBasicProjectionMatrix",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onGetBasicProjectionMatrix(
        Camera camera, float tickDelta, boolean isWorldRendering,
        CallbackInfoReturnable<Matrix4f> cir
    ) {
        if (TransformationManager.isIsometricView) {
            cir.setReturnValue(TransformationManager.getIsometricProjection());
        }
    }
}
