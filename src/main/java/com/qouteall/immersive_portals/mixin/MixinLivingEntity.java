package com.qouteall.immersive_portals.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class MixinLivingEntity {
    @Inject(
        method = "canSee",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onCanSee(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity.world != ((Entity) (Object) this).world) {
            cir.setReturnValue(false);
        }
    }
}
