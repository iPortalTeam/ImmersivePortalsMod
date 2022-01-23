package qouteall.imm_ptl.core.mixin.common;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class MixinLivingEntity {
    
    //maybe avoid memory leak???
    @Inject(method = "Lnet/minecraft/world/entity/LivingEntity;tick()V", at = @At("RETURN"))
    private void onTickEnded(CallbackInfo ci) {
        LivingEntity this_ = (LivingEntity) (Object) this;
        if (this_.getLastHurtByMob() != null) {
            if (this_.getLastHurtByMob().level != this_.level) {
            	this_.setLastHurtByMob(null);
            }
        }
        if (this_.getLastHurtMob() != null) {
            if (this_.getLastHurtMob().level != this_.level) {
            	this_.setLastHurtByPlayer(null);
            }
        }
    }
    
    @Inject(
        method = "Lnet/minecraft/world/entity/LivingEntity;hasLineOfSight(Lnet/minecraft/world/entity/Entity;)Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onCanSee(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity.level != ((Entity) (Object) this).level) {
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
