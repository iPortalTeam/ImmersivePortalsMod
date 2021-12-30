package qouteall.imm_ptl.core.platform_specific;

import net.fabricmc.api.DedicatedServerModInitializer;
import qouteall.imm_ptl.core.compat.IPModCompatibilityWarning;

public class IPModEntryDedicatedServer implements DedicatedServerModInitializer {
    
    @Override
    public void onInitializeServer() {
        IPModCompatibilityWarning.initDedicatedServer();
    }
    
    
}
