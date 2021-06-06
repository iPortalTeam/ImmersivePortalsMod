package qouteall.imm_ptl.core.platform_specific;

import qouteall.q_misc_util.Helper;
import net.fabricmc.api.DedicatedServerModInitializer;

public class IPModEntryDedicatedServer implements DedicatedServerModInitializer {
    
    @Override
    public void onInitializeServer() {
        Helper.log("initializing dedicated server");
    }
    
    
}
