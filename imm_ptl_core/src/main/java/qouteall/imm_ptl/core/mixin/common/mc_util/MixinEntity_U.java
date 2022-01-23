package qouteall.imm_ptl.core.mixin.common.mc_util;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.mc_utils.IPEntityEventListenableEntity;

@Mixin(Entity.class)
public class MixinEntity_U {
    @Inject(
        method = "Lnet/minecraft/world/entity/Entity;setPosRaw(DDD)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/entity/EntityInLevelCallback;onMove()V"
        )
    )
    private void onUpdateEntityPosition(double x, double y, double z, CallbackInfo ci) {
        if (this instanceof IPEntityEventListenableEntity) {
            ((IPEntityEventListenableEntity) this).ip_onEntityPositionUpdated();
        }
    }
    
    @Inject(
        method = "Lnet/minecraft/world/entity/Entity;setRemoved(Lnet/minecraft/world/entity/Entity$RemovalReason;)V",
        at = @At("RETURN")
    )
    private void onSetRemoved(Entity.RemovalReason reason, CallbackInfo ci) {
        if (this instanceof IPEntityEventListenableEntity) {
            ((IPEntityEventListenableEntity) this).ip_onRemoved(reason);
        }
    }
}
