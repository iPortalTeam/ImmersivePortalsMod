package com.qouteall.imm_ptl_peripheral.altius_world;

import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.ModMain;
import net.fabricmc.fabric.api.gamerule.v1.CustomGameRuleCategory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameRules;

public class AltiusGameRule {
    public static CustomGameRuleCategory immersivePortalsCategory = new CustomGameRuleCategory(
            new Identifier("imm_ptl", "altius"),
            new TranslatableText("imm_ptl.altius_screen").styled(style -> style.withBold(true).withColor(Formatting.YELLOW))
    );
    public static GameRules.Key<GameRules.BooleanRule> dimensionStackKey;

//    private static boolean isDimensionStackCache = false;
    
    private static boolean doUpgradeOldDimensionStack = false;
    
    public static void init() {
        dimensionStackKey = GameRuleRegistry.register(
                "ipDimensionStack",
                immersivePortalsCategory,
                GameRuleFactory.createBooleanRule(false)
        );

        ModMain.postServerTickSignal.connect(AltiusGameRule::serverTick);
    }
    
    private static void serverTick() {
//        isDimensionStackCache = getIsDimensionStack();
        
        if (doUpgradeOldDimensionStack) {
            setIsDimensionStack(true);
            AltiusInfo.removeAltius();
            doUpgradeOldDimensionStack = false;
            Helper.log("Upgraded old dimension stack info");
        }
    }
    
    public static boolean getIsDimensionStack() {
        return McHelper.getServer().getGameRules().get(dimensionStackKey).get();
    }
    
    public static void setIsDimensionStack(boolean cond) {
        McHelper.getServer().getGameRules()
            .get(dimensionStackKey).set(cond, McHelper.getServer());
//        isDimensionStackCache = cond;
    }
    
    public static boolean getIsDimensionStackCache() {
        return getIsDimensionStack();
    }
    
    public static void upgradeOldDimensionStack() {
        doUpgradeOldDimensionStack = true;
    }
}
