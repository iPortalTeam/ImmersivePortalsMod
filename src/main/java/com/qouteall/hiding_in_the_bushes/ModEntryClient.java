package com.qouteall.hiding_in_the_bushes;

import com.qouteall.immersive_portals.ModMainClient;
import net.fabricmc.api.ClientModInitializer;

public class ModEntryClient implements ClientModInitializer {
    
    @Override
    public void onInitializeClient() {
        ModMainClient.init();
    }
    
}
