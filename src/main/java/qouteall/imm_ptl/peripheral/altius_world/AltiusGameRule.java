package qouteall.imm_ptl.peripheral.altius_world;

import qouteall.imm_ptl.core.Helper;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.ModMain;
import net.minecraft.world.GameRules;

public class AltiusGameRule {
    public static GameRules.Key<GameRules.BooleanRule> dimensionStackKey;
    
    
    private static boolean doUpgradeOldDimensionStack = false;
    
    public static void init() {
        dimensionStackKey = GameRules.register(
            "ipDimensionStack",
            GameRules.Category.MISC,
            GameRules.BooleanRule.create(false)
        );
        
        ModMain.postServerTickSignal.connect(AltiusGameRule::serverTick);
    }
    
    private static void serverTick() {
        if (doUpgradeOldDimensionStack) {
            setIsDimensionStack(true);
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
    }
    
    public static void upgradeOldDimensionStack() {
        doUpgradeOldDimensionStack = true;
    }
}
