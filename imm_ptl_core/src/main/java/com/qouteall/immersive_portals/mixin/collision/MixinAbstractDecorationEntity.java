package com.qouteall.immersive_portals.mixin.collision;

import com.qouteall.immersive_portals.ducks.IEEntity;
import net.minecraft.entity.decoration.AbstractDecorationEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractDecorationEntity.class)
public class MixinAbstractDecorationEntity {
    @Inject(
        method = "tick",
        at = @At("HEAD")
    )
    private void onTick(CallbackInfo ci) {
        ((IEEntity) this).tickCollidingPortal(1);
    }
}
