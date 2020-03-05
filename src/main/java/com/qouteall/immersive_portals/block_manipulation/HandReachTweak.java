package com.qouteall.immersive_portals.block_manipulation;

import net.minecraft.entity.attribute.ClampedEntityAttribute;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.player.PlayerEntity;

public class HandReachTweak {
    public static final EntityAttribute handReachMultiplier =
        (new ClampedEntityAttribute((EntityAttribute) null, "imm_ptl.hand_reach_multiplier",
            1.0D, 0.0D, 1024.0D
        )).setName("Hand Reach Multiplier").setTracked(true);
    
    public static double getActualHandReachMultiplier(PlayerEntity playerEntity) {
        double multiplier = playerEntity.getAttributeInstance(handReachMultiplier).getValue();
        if (playerEntity.isCreative()) {
            return multiplier * 10;
        }
        else {
            return multiplier;
        }
    }
    
}
