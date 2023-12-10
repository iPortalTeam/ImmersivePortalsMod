package qouteall.q_misc_util;

import net.fabricmc.api.ModInitializer;
import qouteall.q_misc_util.dimension.DimensionIntId;

public class MiscUtilModEntry implements ModInitializer {
    @Override
    public void onInitialize() {
        
        ImplRemoteProcedureCall.init();
        
        MiscNetworking.init();
        
        DimensionIntId.init();
    }
}
