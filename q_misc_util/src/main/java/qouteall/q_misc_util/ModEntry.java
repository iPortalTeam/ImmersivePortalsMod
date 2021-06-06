package qouteall.q_misc_util;

import net.fabricmc.api.ModInitializer;

public class ModEntry implements ModInitializer {
    @Override
    public void onInitialize() {
        IPDimensionAPI.init();
    }
}
