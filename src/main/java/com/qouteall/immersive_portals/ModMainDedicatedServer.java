package com.qouteall.immersive_portals;

import com.qouteall.immersive_portals.my_util.Helper;
import net.fabricmc.api.DedicatedServerModInitializer;

public class ModMainDedicatedServer implements DedicatedServerModInitializer {
    @Override
    public void onInitializeServer() {
        Helper.log("initializing dedicated server");
    }
}
