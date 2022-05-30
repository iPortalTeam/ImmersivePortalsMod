package qouteall.imm_ptl.core.compat.mixin;

import net.coderbot.iris.postprocess.CompositeRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.IPCGlobal;
import qouteall.imm_ptl.core.compat.iris_compatibility.ExperimentalIrisPortalRenderer;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;

@Mixin(CompositeRenderer.class)
public class MixinIrisSodiumCompositeRenderer {
//    @Inject(method = "renderAll", at = @At("HEAD"), cancellable = true)
//    private void onRenderAll(CallbackInfo ci) {
//        if (IPCGlobal.renderer instanceof ExperimentalIrisPortalRenderer) {
//            if (PortalRendering.isRendering()) {
//                ci.cancel();
//            }
//        }
//    }
}
