package qouteall.imm_ptl.core.mixin.client.render.isometric;

import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import qouteall.imm_ptl.core.render.TransformationManager;

@Mixin(GameRenderer.class)
public class MixinGameRenderer_Isometric {
    @Inject(
        method = "getProjectionMatrix",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onGetBasicProjectionMatrix(
        double d,
        CallbackInfoReturnable<Matrix4f> cir
    ) {
        if (TransformationManager.isIsometricView) {
            cir.setReturnValue(TransformationManager.getIsometricProjection());
        }
    }
}
