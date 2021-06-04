package qouteall.imm_ptl.core.platform_specific;

import qouteall.imm_ptl.core.Helper;
import net.fabricmc.api.DedicatedServerModInitializer;

public class ModEntryDedicatedServer implements DedicatedServerModInitializer {
    
    @Override
    public void onInitializeServer() {
        Helper.log("initializing dedicated server");
    }
    
    
}
