package com.qouteall.immersive_portals.block_manipulation;

import com.qouteall.hiding_in_the_bushes.O_O;
import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.PehkuiInterface;
import net.minecraft.entity.attribute.ClampedEntityAttribute;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.player.PlayerEntity;
import org.apache.commons.lang3.Validate;

public class HandReachTweak {
    public static final EntityAttribute handReachMultiplierAttribute =
        (new ClampedEntityAttribute( "imm_ptl.hand_reach_multiplier",
            1.0D, 0.0D, 1024.0D
        )).setTracked(true);
    
    public static double getActualHandReachMultiplier(PlayerEntity playerEntity) {
        if (O_O.isReachEntityAttributesPresent) {
            return 1;
        }
        if (PehkuiInterface.isPehkuiPresent) {
            return 1;
        }
        if (O_O.isForge()) {
            return 1;
        }
        Validate.notNull(playerEntity);
        EntityAttributeInstance instance = playerEntity.getAttributeInstance(handReachMultiplierAttribute);
        if (instance == null) {
            return 1;
        }
        double multiplier = instance.getValue();
        if (Global.longerReachInCreative && playerEntity.isCreative()) {
            return multiplier * 10;
        }
        else {
            return multiplier;
        }
    }
    
    
}
