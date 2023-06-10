package qouteall.imm_ptl.core.compat.mixin;

import net.coderbot.iris.shadows.ShadowRenderTargets;
import org.spongepowered.asm.mixin.Mixin;
import qouteall.imm_ptl.core.compat.iris_compatibility.IEIrisShadowRenderTargets;

@Mixin(value = ShadowRenderTargets.class, remap = false)
public class MixinIrisShadowRenderTargets implements IEIrisShadowRenderTargets {
//    ShadowMapSwapper ip_shadowMapSwapper;
//
//    @Inject(
//        method = "<init>",
//        at = @At("RETURN")
//    )
//    void onInit(int resolution, InternalTextureFormat[] formats, CallbackInfo ci) {
//        ip_shadowMapSwapper = new ShadowMapSwapper(resolution, (ShadowRenderTargets) (Object) this);
//    }
//
//    @Inject(
//        method = "destroy",
//        at = @At("HEAD")
//    )
//    private void onDestroy(CallbackInfo ci) {
//        ip_shadowMapSwapper.dispose();
//        ip_shadowMapSwapper = null;
//    }
//
//    @Override
//    public ShadowMapSwapper getShadowMapSwapper() {
//        return ip_shadowMapSwapper;
//    }
}
