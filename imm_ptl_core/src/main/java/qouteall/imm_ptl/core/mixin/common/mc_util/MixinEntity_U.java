package qouteall.imm_ptl.core.mixin.common.mc_util;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.mc_utils.IPEntityEventListenableEntity;

@Mixin(Entity.class)
public class MixinEntity_U {
    @Inject(
        method = "setPos",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/EntityChangeListener;updateEntityPosition()V"
        )
    )
    private void onUpdateEntityPosition(double x, double y, double z, CallbackInfo ci) {
        if (this instanceof IPEntityEventListenableEntity) {
            ((IPEntityEventListenableEntity) this).ip_onEntityPositionUpdated();
        }
    }
    
    @Inject(
        method = "setRemoved",
        at = @At("RETURN")
    )
    private void onSetRemoved(Entity.RemovalReason reason, CallbackInfo ci) {
        if (this instanceof IPEntityEventListenableEntity) {
            ((IPEntityEventListenableEntity) this).ip_onRemoved(reason);
        }
    }
}
