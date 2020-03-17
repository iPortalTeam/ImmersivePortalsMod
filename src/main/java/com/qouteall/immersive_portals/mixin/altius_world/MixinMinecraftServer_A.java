package com.qouteall.immersive_portals.mixin.altius_world;

import com.qouteall.immersive_portals.altius_world.AltiusGeneratorType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.level.LevelGeneratorType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer_A {
    @Shadow
    public abstract ServerWorld getWorld(DimensionType dimensionType);
    
    @Inject(
        method = "prepareStartRegion",
        at = @At("RETURN")
    )
    private void onStartRegionPrepared(
        WorldGenerationProgressListener worldGenerationProgressListener,
        CallbackInfo ci
    ) {
        LevelGeneratorType genType = getWorld(DimensionType.OVERWORLD).getLevelProperties().getGeneratorType();
        if (genType == AltiusGeneratorType.generatorType) {
            AltiusGeneratorType.initializePortalsIfNecessary();
        }
    }
}
