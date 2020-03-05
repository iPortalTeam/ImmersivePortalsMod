package com.qouteall.hiding_in_the_bushes;

import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.ModMainClient;
import com.qouteall.immersive_portals.OFInterface;
import com.qouteall.immersive_portals.optifine_compatibility.OFBuiltChunkNeighborFix;
import com.qouteall.immersive_portals.optifine_compatibility.OFInterfaceInitializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.FabricLoader;

public class ModEntryClient implements ClientModInitializer {
    
    @Override
    public void onInitializeClient() {
        ModMainClient.init();
    
        OFInterface.isOptifinePresent = FabricLoader.INSTANCE.isModLoaded("optifabric");
    
        if (OFInterface.isOptifinePresent) {
            OFBuiltChunkNeighborFix.init();
            OFInterfaceInitializer.init();
        }
    
        Helper.log(OFInterface.isOptifinePresent ? "Optifine is present" : "Optifine is not present");
    }
    
}
