package com.qouteall.hiding_in_the_bushes;

import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.ModMain;
import net.fabricmc.api.ModInitializer;

public class ModEntry implements ModInitializer {
    
    @Override
    public void onInitialize() {
        ModMain.init();
        RequiemCompat.init();
    
        Helper.log("Start Registering");
    
        MyRegistry.registerEntitiesFabric();
    
        Helper.log("Entities Registered");
    
        MyRegistry.registerMyDimensionsFabric();
    
        Helper.log("Dimensions Registered");
    
        MyRegistry.registerBlocksFabric();
    
        Helper.log("Blocks Registered");
    
        MyRegistry.registerEffectAndPotion();
    
        Helper.log("Status Effects Registered");
    }
    
}
