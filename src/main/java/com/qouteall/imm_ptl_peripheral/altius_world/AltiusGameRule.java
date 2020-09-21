package com.qouteall.imm_ptl_peripheral.altius_world;

import com.qouteall.immersive_portals.McHelper;
import net.minecraft.world.GameRules;

public class AltiusGameRule {
    public static GameRules.Key<GameRules.BooleanRule> dimensionStackKey;
    
    private static boolean isDimensionStackCache = false;
    
    public static void init(){
        dimensionStackKey = GameRules.register(
            "ipDimensionStack",
            GameRules.Category.MISC,
            GameRules.BooleanRule.create(false)
        );
    }
    
    public static boolean getIsDimensionStack(){
        return McHelper.getServer().getGameRules().getBoolean(dimensionStackKey);
    }
    
    public static void setIsDimensionStack(boolean cond) {
        McHelper.getServer().getGameRules()
            .get(dimensionStackKey).set(cond, McHelper.getServer());
    }
}
