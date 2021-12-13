package qouteall.imm_ptl.core.mixin.client.debug.isometric;

import net.minecraft.client.render.GameRenderer;
import net.minecraft.util.math.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import qouteall.imm_ptl.core.render.TransformationManager;

@Mixin(GameRenderer.class)
public class MixinGameRenderer_I {
    @Inject(
        method = "getBasicProjectionMatrix",
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
