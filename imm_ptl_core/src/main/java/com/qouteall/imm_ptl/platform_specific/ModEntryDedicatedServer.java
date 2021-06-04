package com.qouteall.imm_ptl.platform_specific;

import com.qouteall.immersive_portals.Helper;
import net.fabricmc.api.DedicatedServerModInitializer;

public class ModEntryDedicatedServer implements DedicatedServerModInitializer {
    
    @Override
    public void onInitializeServer() {
        Helper.log("initializing dedicated server");
    }
    
    
}
