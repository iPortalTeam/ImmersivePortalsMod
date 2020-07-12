package com.qouteall.hiding_in_the_bushes;

import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.ModMain;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.FabricLoader;
import net.logandark.languagehack.LanguageHack;

public class ModEntry implements ModInitializer {
    
    @Override
    public void onInitialize() {
        ModMain.init();
        RequiemCompat.init();
        
        MyRegistry.registerEntitiesFabric();
        
        MyRegistry.registerMyDimensionsFabric();
        
        MyRegistry.registerBlocksFabric();
        
        MyRegistry.registerEffectAndPotion();
        
        MyRegistry.registerChunkGenerators();
        
        O_O.isReachEntityAttributesPresent = FabricLoader.INSTANCE.isModLoaded(
            "reach-entity-attributes");
        if (O_O.isReachEntityAttributesPresent) {
            Helper.log("Reach entity attributes mod is present");
        }
        else {
            Helper.log("Reach entity attributes mod is not present");
        }
        
        LanguageHack.activate("immersive_portals");
    }
    
}
