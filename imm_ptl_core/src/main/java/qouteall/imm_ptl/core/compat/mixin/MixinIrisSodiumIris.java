package qouteall.imm_ptl.core.compat.mixin;

import net.coderbot.iris.Iris;
import net.coderbot.iris.shaderpack.DimensionId;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import qouteall.imm_ptl.core.IPCGlobal;
import qouteall.imm_ptl.core.compat.iris_compatibility.ExperimentalIrisPortalRenderer;

@Mixin(Iris.class)
public class MixinIrisSodiumIris {
    // test
    // only overworld
    @Inject(
        method = "getCurrentDimension", at = @At("HEAD"), cancellable = true
    )
    private static void onGetCurrentDimension(CallbackInfoReturnable<DimensionId> cir) {
        if (IPCGlobal.renderer instanceof ExperimentalIrisPortalRenderer) {
            cir.setReturnValue(DimensionId.OVERWORLD);
        }
    }
}
