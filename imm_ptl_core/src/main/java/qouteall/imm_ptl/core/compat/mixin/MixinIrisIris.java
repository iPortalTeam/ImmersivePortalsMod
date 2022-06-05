package qouteall.imm_ptl.core.compat.mixin;

import net.coderbot.iris.Iris;
import net.coderbot.iris.shaderpack.DimensionId;
import net.fabricmc.loader.api.FabricLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import qouteall.imm_ptl.core.IPCGlobal;
import qouteall.imm_ptl.core.compat.iris_compatibility.ExperimentalIrisPortalRenderer;

@Mixin(value = Iris.class, remap = false)
public class MixinIrisIris {
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
    
//    // it cannot recognize sodium from jitpack
//    @Inject(
//        method = "isSodiumInvalid", at = @At("HEAD"), cancellable = true
//    )
//    private static void onIsSodiumInvalid(CallbackInfoReturnable<Boolean> cir) {
//        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
//            cir.setReturnValue(false);
//        }
//    }
}
