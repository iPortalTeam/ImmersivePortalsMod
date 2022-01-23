package qouteall.imm_ptl.peripheral.altius_world;

import net.minecraft.world.level.GameRules;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.q_misc_util.MiscHelper;

public class AltiusGameRule {
    public static GameRules.Key<GameRules.BooleanValue> dimensionStackKey;
    
    
    private static boolean doUpgradeOldDimensionStack = false;
    
    public static void init() {
        dimensionStackKey = GameRules.register(
            "ipDimensionStack",
            GameRules.Category.MISC,
            GameRules.BooleanValue.create(false)
        );
        
        IPGlobal.postServerTickSignal.connect(AltiusGameRule::serverTick);
    }
    
    private static void serverTick() {
        if (doUpgradeOldDimensionStack) {
            AltiusManagement.upgradeLegacyDimensionStack(MiscHelper.getServer());
            doUpgradeOldDimensionStack = false;
        }
    }
    
    public static boolean getIsDimensionStack() {
        return MiscHelper.getServer().getGameRules().getRule(dimensionStackKey).get();
    }
    
    public static void setIsDimensionStack(boolean cond) {
        MiscHelper.getServer().getGameRules()
            .getRule(dimensionStackKey).set(cond, MiscHelper.getServer());
    }
    
    public static void upgradeOldDimensionStack() {
        doUpgradeOldDimensionStack = true;
    }
}
