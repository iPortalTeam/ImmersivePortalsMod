package qouteall.imm_ptl.core.platform_specific;

import qouteall.q_misc_util.Helper;
import qouteall.imm_ptl.core.IPModMain;
import qouteall.imm_ptl.core.PehkuiInterface;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public class IPModEntry implements ModInitializer {
    
    @Override
    public void onInitialize() {
        IPModMain.init();
        RequiemCompat.init();
        
        IPRegistry.registerEntitiesFabric();
        
        IPRegistry.registerMyDimensionsFabric();
        
        IPRegistry.registerBlocksFabric();
        
        IPRegistry.registerChunkGenerators();
        
        O_O.isReachEntityAttributesPresent =
            FabricLoader.getInstance().isModLoaded("reach-entity-attributes");
        if (O_O.isReachEntityAttributesPresent) {
            Helper.log("Reach entity attributes mod is present");
        }
        else {
            Helper.log("Reach entity attributes mod is not present");
        }
        
        PehkuiInterface.isPehkuiPresent =
            O_O.getIsPehkuiPresent();
        if (PehkuiInterface.isPehkuiPresent) {
            PehkuiInterfaceInitializer.init();
            Helper.log("Pehkui is present");
        }
        else {
            Helper.log("Pehkui is not present");
        }
        
        
    }
    
}
