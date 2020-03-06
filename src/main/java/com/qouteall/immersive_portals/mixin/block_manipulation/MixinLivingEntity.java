package com.qouteall.immersive_portals.mixin.block_manipulation;

import com.qouteall.immersive_portals.block_manipulation.HandReachTweak;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.AbstractEntityAttributeContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity {
    @Shadow public abstract AbstractEntityAttributeContainer getAttributes();
    
    @Inject(
        method = "initAttributes",
        at = @At("TAIL")
    )
    private void onInitAttributes(CallbackInfo ci) {
        getAttributes().register(HandReachTweak.handReachMultiplierAttribute);
    }
}
