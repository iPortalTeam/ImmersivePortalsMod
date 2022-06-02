package qouteall.imm_ptl.core.compat.mixin;

import net.coderbot.iris.pipeline.newshader.NewWorldRenderingPipeline;
import net.coderbot.iris.shadows.ShadowRenderTargets;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.IPCGlobal;
import qouteall.imm_ptl.core.compat.iris_compatibility.ExperimentalIrisPortalRenderer;
import qouteall.imm_ptl.core.compat.iris_compatibility.IEIrisNewWorldRenderingPipeline;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;

@Mixin(NewWorldRenderingPipeline.class)
public class MixinIrisNewWorldRenderingPipeline implements IEIrisNewWorldRenderingPipeline {
    @Shadow private boolean isRenderingWorld;
    
//    @Shadow private ShadowRenderTargets shadowRenderTargets;
    
    @Inject(
        method = "finalizeLevelRendering", at = @At("HEAD"), cancellable = true
    )
    private void onFinalizeLevelRendering(CallbackInfo ci) {
        if (IPCGlobal.renderer instanceof ExperimentalIrisPortalRenderer) {
            if (PortalRendering.isRendering()) {
                ci.cancel();
            }
        }
    }
    
    @Inject(
        method = "beginTranslucents",
        at = @At(
            value = "INVOKE",
            target = "Lnet/coderbot/iris/postprocess/CompositeRenderer;renderAll()V",
            shift = At.Shift.AFTER
        )
    )
    private void onAfterDeferredCompositeRendering(CallbackInfo ci) {
        if (IPCGlobal.renderer instanceof ExperimentalIrisPortalRenderer r) {
            r.onAfterIrisDeferredCompositeRendering();
        }
    }
    
    @Override
    public void ip_setIsRenderingWorld(boolean cond) {
        isRenderingWorld = cond;
    }
    
//    @Override
//    public ShadowRenderTargets ip_getShadowRenderTargets() {
//        return shadowRenderTargets;
//    }
}
