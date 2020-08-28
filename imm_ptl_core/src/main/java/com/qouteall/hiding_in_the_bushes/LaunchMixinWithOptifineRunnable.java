package com.qouteall.hiding_in_the_bushes;

import net.fabricmc.loader.FabricLoader;
import org.spongepowered.asm.mixin.Mixins;

public class LaunchMixinWithOptifineRunnable implements Runnable {
    @Override
    public void run() {
        if (FabricLoader.INSTANCE.isModLoaded("optifabric")) {
            Mixins.addConfiguration("immersive_portals.mixins_with_optifine.json");
        }
    }
}
