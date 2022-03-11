package qouteall.q_misc_util;

import net.fabricmc.api.ModInitializer;
import qouteall.q_misc_util.dimension.DimensionMisc;
import qouteall.q_misc_util.dimension.ExtraDimensionStorage;

public class MiscUtilModEntry implements ModInitializer {
    @Override
    public void onInitialize() {
        DimensionMisc.init();
    
        ExtraDimensionStorage.init();
        
        MiscNetworking.init();
    }
}
