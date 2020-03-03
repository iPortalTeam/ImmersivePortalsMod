package com.qouteall.modloader_agnostic_api;

import com.qouteall.immersive_portals.Helper;
import net.fabricmc.api.DedicatedServerModInitializer;

public class ModEntryDedicatedServer implements DedicatedServerModInitializer {
    
    @Override
    public void onInitializeServer() {
        Helper.log("initializing dedicated server");
    }
    
    
}
