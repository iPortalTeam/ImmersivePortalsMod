package qouteall.imm_ptl.core.compat.mixin.flywheel;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.ClientWorldLoader;

@Pseudo
@Mixin(targets = "com.jozufozu.flywheel.core.crumbling.CrumblingRenderer", remap = false)
public class MixinFlywheelCrumblingRenderer {
    
    @Inject(
        method = "onReloadRenderers", at = @At("HEAD"),
        cancellable = true
    )
    private static void onInvalidateAll(@Coerce Object obj, CallbackInfo ci) {
        if (ClientWorldLoader.getIsCreatingClientWorld()) {
            ci.cancel();
        }
    }
}
