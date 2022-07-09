package qouteall.imm_ptl.core.mixin.client.render.isometric;

import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import qouteall.imm_ptl.core.render.TransformationManager;

@Mixin(Frustum.class)
public class MixinFrustum_Isometric {
    // avoid dead loop
    @Inject(
        method = "offsetToFullyIncludeCameraCube", at = @At("HEAD"), cancellable = true
    )
    private void onOffsetToFullyIncludeCameraCube(int i, CallbackInfoReturnable<Frustum> cir) {
        if (TransformationManager.isIsometricView) {
            cir.setReturnValue((Frustum) (Object) this);
        }
    }
}
