package qouteall.imm_ptl.core.miscellaneous;

import qouteall.imm_ptl.core.IPGlobal;

public class DubiousThings {
    public static void init() {
        IPGlobal.postClientTickSignal.connect(DubiousThings::tick);
    }
    
    private static void tick() {
        // things removed
    }
    
}
