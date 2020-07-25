package com.qouteall.hiding_in_the_bushes;

import com.qouteall.hiding_in_the_bushes.sodium_compatibility.SodiumInterfaceInitializer;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.ModMain;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
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
        
        O_O.isReachEntityAttributesPresent =
            FabricLoader.getInstance().isModLoaded("reach-entity-attributes");
        if (O_O.isReachEntityAttributesPresent) {
            Helper.log("Reach entity attributes mod is present");
        }
        else {
            Helper.log("Reach entity attributes mod is not present");
        }
        
        SodiumInterface.isSodiumPresent =
            FabricLoader.getInstance().isModLoaded("sodium");
        if (SodiumInterface.isSodiumPresent) {
            Helper.log("Sodium is present");
            SodiumInterfaceInitializer.init();
        }
        else {
            Helper.log("Sodium is not present");
        }
        
        LanguageHack.activate("immersive_portals");
    }
    
}
