package com.qouteall.imm_ptl_peripheral.mixin.common.altius_world;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer_A {
    @Shadow
    public abstract ServerWorld getWorld(RegistryKey<World> dimensionType);

//    @Inject(
//        method = "prepareStartRegion",
//        at = @At("RETURN")
//    )
//    private void onStartRegionPrepared(
//        WorldGenerationProgressListener worldGenerationProgressListener,
//        CallbackInfo ci
//    ) {
//        AltiusInfo info = AltiusInfo.getInfoFromServer();
//        if (info != null) {
//            info.createPortals();
//        }
//    }
}
