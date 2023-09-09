package qouteall.imm_ptl.core.platform_specific;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import qouteall.imm_ptl.core.IPModMain;
import qouteall.imm_ptl.core.compat.GravityChangerInterface;
import qouteall.q_misc_util.Helper;

public class IPModEntry implements ModInitializer {
    
    @Override
    public void onInitialize() {
        IPModMain.init();
        RequiemCompat.init();
        
        IPModMain.registerEntityTypes(
            (id, entityType) -> Registry.register(BuiltInRegistries.ENTITY_TYPE, id, entityType)
        );
        
        IPModMain.registerBlocks((id, obj) -> Registry.register(BuiltInRegistries.BLOCK, id, obj));
        
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
        
        if (FabricLoader.getInstance().isModLoaded("gravity_changer_q")) {
            GravityChangerInterface.invoker = new GravityChangerInterface.OnGravityChangerPresent();
            Helper.log("Gravity API is present");
        }
        else {
            Helper.log("Gravity API is not present");
        }
        
    }
    
}
