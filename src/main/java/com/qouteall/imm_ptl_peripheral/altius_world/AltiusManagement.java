package com.qouteall.imm_ptl_peripheral.altius_world;

import com.qouteall.immersive_portals.ModMain;

public class AltiusManagement {
    // Dimension stack world can only be created in client
    // This is assigned in client
    public static AltiusInfo dimensionStackPortalsToGenerate = null;
    
    public static void init() {
        ModMain.postServerTickSignal.connect(AltiusManagement::serverTick);
    }
    
    private static void serverTick() {
        if (dimensionStackPortalsToGenerate != null) {
            dimensionStackPortalsToGenerate.createPortals();
            dimensionStackPortalsToGenerate = null;
            AltiusGameRule.setIsDimensionStack(true);
        }
    }
}
