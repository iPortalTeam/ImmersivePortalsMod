package com.qouteall.imm_ptl_peripheral.mixin.common.alternate_dimension;

import net.minecraft.entity.ai.goal.AvoidSunlightGoal;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(AvoidSunlightGoal.class)
public class MixinAvoidSunlightGoal {
//    @Shadow
//    @Final
//    private PathAwareEntity mob;
//
//    //fix crash
//    @Inject(
//        method = "stop",
//        at = @At("HEAD"),
//        cancellable = true
//    )
//    private void onStop(CallbackInfo ci) {
//        if (!(mob.getNavigation() instanceof MobNavigation)) {
//            Helper.err("Avoid sunlight goal abnormal");
//            ci.cancel();
//        }
//    }
}
