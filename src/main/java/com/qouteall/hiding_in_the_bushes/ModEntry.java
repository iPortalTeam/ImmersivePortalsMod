package com.qouteall.hiding_in_the_bushes;

import com.qouteall.immersive_portals.ModMain;
import net.fabricmc.api.ModInitializer;

public class ModEntry implements ModInitializer {
    
    @Override
    public void onInitialize() {
        ModMain.init();
        RequiemCompat.init();
    
        MyRegistry.registerEntitiesFabric();
        MyRegistry.registerMyDimensionsFabric();
        MyRegistry.registerBlocksFabric();
    }
    
}
