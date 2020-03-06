package com.qouteall.immersive_portals.block_manipulation;

import com.qouteall.immersive_portals.Global;
import net.minecraft.entity.attribute.ClampedEntityAttribute;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.Potion;

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
    
    
}
