package com.qouteall.immersive_portals.mixin.block_manipulation;

import com.qouteall.immersive_portals.block_manipulation.HandReachTweak;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(StatusEffect.class)
public class MixinStatusEffect {
    @Invoker("<init>")
    private static StatusEffect construct(StatusEffectType type, int color) {
        return null;
    }
    
    static {
        HandReachTweak.statusEffectConstructor = (a, b) -> construct(a, b);
    }
}
