package qouteall.imm_ptl.core.mixin.client.render;

import com.mojang.math.Matrix4f;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import qouteall.imm_ptl.core.render.TransformationManager;

@Mixin(GameRenderer.class)
public class MixinGameRenderer_Isometric {
    @Inject(
        method = "Lnet/minecraft/client/renderer/GameRenderer;getProjectionMatrix(D)Lcom/mojang/math/Matrix4f;",
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
