package qouteall.imm_ptl.core.platform_specific;

import qouteall.imm_ptl.core.compat.GravityChangerInterface;
import qouteall.imm_ptl.core.compat.iris_compatibility.IrisInterface;
import qouteall.q_misc_util.Helper;
import qouteall.imm_ptl.core.IPModMain;
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
        
        if (FabricLoader.getInstance().isModLoaded("dimthread")) {
            O_O.isDimensionalThreadingPresent = true;
            Helper.log("Dimensional Threading is present");
        }
        else {
            Helper.log("Dimensional Threading is not present");
        }
        
        if (O_O.getIsPehkuiPresent()) {
            PehkuiInterfaceInitializer.init();
            Helper.log("Pehkui is present");
        }
        else {
            Helper.log("Pehkui is not present");
        }
        
        if (FabricLoader.getInstance().isModLoaded("gravitychanger")) {
            GravityChangerInterface.invoker = new GravityChangerInterface.OnGravityChangerPresent();
            Helper.log("Gravity Changer is present");
        }
        else {
            Helper.log("Gravity Changer is not present");
        }
        
    }
    
}
