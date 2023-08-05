package qouteall.imm_ptl.core.compat.mixin.iris;

import net.coderbot.iris.pipeline.ClearPass;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.IPCGlobal;
import qouteall.imm_ptl.core.compat.iris_compatibility.ExperimentalIrisPortalRenderer;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;

@Mixin(value = ClearPass.class, remap = false)
public class MixinIrisClearPass {
    @Inject(
        method = "execute", at = @At("HEAD"), cancellable = true
    )
    private void onExecute(Vector4f par1, CallbackInfo ci) {
        if (IPCGlobal.renderer instanceof ExperimentalIrisPortalRenderer) {
            if (PortalRendering.isRendering()) {
                ci.cancel();
            }
        }
    }
}
