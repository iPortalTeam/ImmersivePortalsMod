package com.qouteall.immersive_portals.mixin.block_manipulation;

import com.qouteall.imm_ptl_peripheral.block_manipulation.HandReachTweak;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public class MixinPlayerEntity_B {
    @Inject(method = "createPlayerAttributes", at = @At("RETURN"), cancellable = true)
    private static void onCreatePlayerAttributes(
        CallbackInfoReturnable<DefaultAttributeContainer.Builder> cir
    ) {
        cir.setReturnValue(
            cir.getReturnValue().add(
                HandReachTweak.handReachMultiplierAttribute,
                1.0
            )
        );
    }
}
