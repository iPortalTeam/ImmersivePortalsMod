package com.qouteall.immersive_portals.mixin.common.mc_util;

import com.qouteall.immersive_portals.mc_utils.IPEntityEventListenableEntity;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
