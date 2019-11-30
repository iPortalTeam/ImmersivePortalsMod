package com.qouteall.immersive_portals.optifine_compatibility;

import net.optifine.Config;
import net.optifine.shaders.Shaders;

@Deprecated
public class OFInterfaceInitializer {
    public static void init() {
        OFInterface.isOptifinePresent = () -> true;
        OFInterface.isShaders = Config::isShaders;
        OFInterface.isShadowPass = () -> Config.isShaders() && Shaders.isShadowPass;
        
    }
}
