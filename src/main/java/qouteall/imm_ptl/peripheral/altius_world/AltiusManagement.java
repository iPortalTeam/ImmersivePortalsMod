package qouteall.imm_ptl.peripheral.altius_world;

import qouteall.imm_ptl.core.IPGlobal;

public class AltiusManagement {
    // Dimension stack world can only be created in client
    // This is assigned in client
    public static AltiusInfo dimensionStackPortalsToGenerate = null;
    
    public static void init() {
        IPGlobal.postServerTickSignal.connect(AltiusManagement::serverTick);
    }
    
    private static void serverTick() {
        if (dimensionStackPortalsToGenerate != null) {
            dimensionStackPortalsToGenerate.createPortals();
            dimensionStackPortalsToGenerate = null;
            AltiusGameRule.setIsDimensionStack(true);
        }
    }
}
