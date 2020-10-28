package com.qouteall.hiding_in_the_bushes;

import net.fabricmc.loader.api.FabricLoader;
import org.spongepowered.asm.mixin.Mixins;

public class LaunchMixinWithOptifineRunnable implements Runnable {
    @Override
    public void run() {
        if (FabricLoader.getInstance().isModLoaded("optifabric")) {
            System.out.println("Registering Mixin for OptiFine");//Helper.log may load classes
            Mixins.addConfiguration("imm_ptl_optifine.mixins.json");
        }
    }
}
