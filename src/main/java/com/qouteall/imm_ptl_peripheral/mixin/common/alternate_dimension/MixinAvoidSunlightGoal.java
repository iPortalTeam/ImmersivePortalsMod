package com.qouteall.imm_ptl_peripheral.mixin.common.alternate_dimension;

import com.qouteall.immersive_portals.Helper;
import net.minecraft.entity.ai.goal.AvoidSunlightGoal;
import net.minecraft.entity.ai.pathing.MobNavigation;
import net.minecraft.entity.mob.PathAwareEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
