package com.qouteall.immersive_portals.mixin.common;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class MixinLivingEntity {
    
    //maybe avoid memory leak???
    @Inject(method = "tick", at = @At("RETURN"))
    private void onTickEnded(CallbackInfo ci) {
        LivingEntity this_ = (LivingEntity) (Object) this;
        if (this_.getAttacker() != null) {
            if (this_.getAttacker().world != this_.world) {
            	this_.setAttacker(null);
            }
        }
        if (this_.getAttacking() != null) {
            if (this_.getAttacking().world != this_.world) {
            	this_.setAttacking(null);
            }
        }
    }
    
    @Inject(
        method = "canSee",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onCanSee(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity.world != ((Entity) (Object) this).world) {
            cir.setReturnValue(false);
            return;
        }
    }
    
//    @Inject(
//        method = "canSee",
//        at = @At("RETURN"),
//        cancellable = true
//    )
//    private void onCanSeeReturns(Entity entity, CallbackInfoReturnable<Boolean> cir) {
//        LivingEntity this_ = (LivingEntity) (Object) this;
//        if (cir.getReturnValue()) {
//            if (Portal.doesPortalBlockEntityView(this_, entity)) {
//                cir.setReturnValue(false);
//            }
//        }
//    }
}
