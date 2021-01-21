package com.qouteall.immersive_portals.block_manipulation;

import com.qouteall.hiding_in_the_bushes.O_O;
import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.PehkuiInterface;
import net.minecraft.entity.player.PlayerEntity;
import org.apache.commons.lang3.Validate;

public class HandReachTweak {
    
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
        
        if (Global.longerReachInCreative && playerEntity.isCreative()) {
            return 10;
        }
        else {
            return 1;
        }
    }
    
    
}
