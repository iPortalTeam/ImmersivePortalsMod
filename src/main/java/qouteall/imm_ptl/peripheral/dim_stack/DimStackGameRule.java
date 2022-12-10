package qouteall.imm_ptl.peripheral.dim_stack;

import net.minecraft.world.level.GameRules;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.q_misc_util.MiscHelper;

// the dimension stack information is stored together with global portal storage now
// still add the gamerule to ensure that old dim stack worlds can upgrade
// TODO remove in 1.20
@Deprecated
public class DimStackGameRule {
    public static GameRules.Key<GameRules.BooleanValue> dimensionStackKey;
    
    
    private static boolean doUpgradeOldDimensionStack = false;
    
    public static void init() {
        dimensionStackKey = GameRules.register(
            "ipDimensionStack",
            GameRules.Category.MISC,
            GameRules.BooleanValue.create(false)
        );
        
        IPGlobal.postServerTickSignal.connect(DimStackGameRule::serverTick);
    }
    
    private static void serverTick() {
        if (doUpgradeOldDimensionStack) {
            DimStackManagement.upgradeLegacyDimensionStack(MiscHelper.getServer());
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
