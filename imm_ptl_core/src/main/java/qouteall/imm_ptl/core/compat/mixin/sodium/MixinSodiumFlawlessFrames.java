package qouteall.imm_ptl.core.compat.mixin.sodium;

import me.jellysquid.mods.sodium.client.util.FlawlessFrames;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import qouteall.imm_ptl.core.render.ForceMainThreadRebuild;

@Mixin(FlawlessFrames.class)
public class MixinSodiumFlawlessFrames {
    @Inject(method = "isActive", at = @At("HEAD"), cancellable = true)
    private static void onIsActive(CallbackInfoReturnable<Boolean> cir) {
        if (ForceMainThreadRebuild.isCurrentFrameForceMainThreadRebuild()) {
            cir.setReturnValue(true);
        }
    }
}
