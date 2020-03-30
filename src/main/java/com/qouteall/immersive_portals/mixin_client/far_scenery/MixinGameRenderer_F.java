package com.qouteall.immersive_portals.mixin_client.far_scenery;

import net.minecraft.client.render.GameRenderer;
import net.minecraft.util.math.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GameRenderer.class)
public class MixinGameRenderer_F {
    @Shadow
    private boolean renderingPanorama;
    
    @Redirect(
        method = "getBasicProjectionMatrix",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/math/Matrix4f;viewboxMatrix(DFFF)Lnet/minecraft/util/math/Matrix4f;"
        )
    )
    Matrix4f redirectProjectionMatrix(
        double fov,
        float aspectRatio,
        float cameraDepth,
        float viewDistance
    ) {
        if (renderingPanorama) {
            return Matrix4f.viewboxMatrix(
                90,
                1,
                0.05F,
                viewDistance
            );
        }
        else {
            return Matrix4f.viewboxMatrix(
                fov, aspectRatio, cameraDepth, viewDistance
            );
        }
    }
}
