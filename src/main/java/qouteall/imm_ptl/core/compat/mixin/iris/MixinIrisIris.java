package qouteall.imm_ptl.core.compat.mixin.iris;

import net.coderbot.iris.Iris;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = Iris.class, remap = false)
public class MixinIrisIris {
    // test
    // only overworld
//    @Inject(
//        method = "getCurrentDimension", at = @At("HEAD"), cancellable = true
//    )
//    private static void onGetCurrentDimension(CallbackInfoReturnable<DimensionId> cir) {
//        if (IPCGlobal.renderer instanceof ExperimentalIrisPortalRenderer) {
//            cir.setReturnValue(DimensionId.OVERWORLD);
//        }
//    }
    
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
