package com.qouteall.immersive_portals.block_manipulation;

import com.qouteall.immersive_portals.Global;
import net.minecraft.entity.attribute.ClampedEntityAttribute;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.Potion;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.util.function.BiFunction;

public class HandReachTweak {
    public static BiFunction<StatusEffectType, Integer, StatusEffect>
        statusEffectConstructor;
    
    public static final EntityAttribute handReachMultiplierAttribute =
        (new ClampedEntityAttribute((EntityAttribute) null, "imm_ptl.hand_reach_multiplier",
            1.0D, 0.0D, 1024.0D
        )).setName("Hand Reach Multiplier").setTracked(true);
    
    public static StatusEffect longerReachEffect;
    
    public static Potion longerReachPotion;
    
    public static double getActualHandReachMultiplier(PlayerEntity playerEntity) {
        double multiplier = playerEntity.getAttributeInstance(handReachMultiplierAttribute).getValue();
        if (Global.longerReachInCreative && playerEntity.isCreative()) {
            return multiplier * 10;
        }
        else {
            return multiplier;
        }
    }
    
    
    
    public static void init() {
        StatusEffect.class.hashCode();
        longerReachEffect = statusEffectConstructor.apply(StatusEffectType.BENEFICIAL, 0)
            .addAttributeModifier(
                handReachMultiplierAttribute,
                "91AEAA56-2333-2333-2333-2F7F68070635",
                0.5,
                EntityAttributeModifier.Operation.MULTIPLY_TOTAL
            );
        Registry.register(
            Registry.STATUS_EFFECT,
            new Identifier("immersive_portals", "longer_reach"),
            longerReachEffect
        );
    
        longerReachPotion = new Potion(
            new StatusEffectInstance(
                longerReachEffect, 3600, 1
            )
        );
        Registry.register(
            Registry.POTION,
            new Identifier("immersive_portals", "longer_reach_potion"),
            longerReachPotion
        );
    }
    
    
}
