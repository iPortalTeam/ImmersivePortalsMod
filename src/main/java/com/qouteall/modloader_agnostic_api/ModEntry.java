package com.qouteall.modloader_agnostic_api;

import com.qouteall.immersive_portals.ModMain;
import net.fabricmc.api.ModInitializer;

public class ModEntry implements ModInitializer {
    
    
    @Override
    public void onInitialize() {
        ModMain.init();
    }
    
}
