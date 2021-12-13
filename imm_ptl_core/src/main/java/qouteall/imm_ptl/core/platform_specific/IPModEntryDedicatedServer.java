package qouteall.imm_ptl.core.platform_specific;

import net.fabricmc.api.DedicatedServerModInitializer;
import qouteall.q_misc_util.Helper;

public class IPModEntryDedicatedServer implements DedicatedServerModInitializer {
    
    @Override
    public void onInitializeServer() {
        Helper.log("initializing dedicated server");
    }
    
    
}
