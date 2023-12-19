package qouteall.imm_ptl.core.miscellaneous;

import qouteall.imm_ptl.core.IPGlobal;

public class DubiousThings {
    public static void init() {
        IPGlobal.POST_CLIENT_TICK_EVENT.register(DubiousThings::tick);
    }
    
    private static void tick() {
        // things removed
    }
    
}
