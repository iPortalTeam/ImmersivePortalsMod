package com.qouteall.hiding_in_the_bushes;

import com.qouteall.immersive_portals.Helper;
import net.fabricmc.loader.FabricLoader;
import org.spongepowered.asm.mixin.Mixins;

public class LaunchMixinWithOptifineRunnable implements Runnable {
    @Override
    public void run() {
        Helper.log("oops");
        if (FabricLoader.INSTANCE.isModLoaded("optifabric")) {
            Helper.log("Registering Mixin for OptiFine");
            Mixins.addConfiguration("imm_ptl_optifine.mixins.json");
        }
    }
}
