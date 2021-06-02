package com.qouteall.immersive_portals.mixin.common.dimension;

import com.mojang.serialization.Lifecycle;
import com.qouteall.immersive_portals.api.IPDimensionAPI;
import com.qouteall.immersive_portals.ducks.IEGeneratorOptions;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.level.LevelProperties;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelProperties.class)
public class MixinLevelProperties_D {
    @Shadow
    @Final
    private GeneratorOptions generatorOptions;
    
    @Shadow
    @Final
    @Mutable
    private Lifecycle lifecycle;
    
//    @Inject(
//        method = "<init>(Lcom/mojang/datafixers/DataFixer;ILnet/minecraft/nbt/CompoundTag;ZIIIFJJIIIZIZZZLnet/minecraft/world/border/WorldBorder$Properties;IILjava/util/UUID;Ljava/util/LinkedHashSet;Lnet/minecraft/world/timer/Timer;Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/world/level/LevelInfo;Lnet/minecraft/world/gen/GeneratorOptions;Lcom/mojang/serialization/Lifecycle;)V",
//        at = @At("RETURN")
//    )
//    private void onConstructedFromLevelInfo(
//        DataFixer dataFixer, int dataVersion, CompoundTag playerData,
//        boolean modded, int spawnX, int spawnY, int spawnZ, float spawnAngle,
//        long time, long timeOfDay, int version, int clearWeatherTime, int rainTime,
//        boolean raining, int thunderTime, boolean thundering, boolean initialized,
//        boolean difficultyLocked, WorldBorder.Properties worldBorder, int wanderingTraderSpawnDelay,
//        int wanderingTraderSpawnChance, UUID wanderingTraderId, LinkedHashSet<String> serverBrands,
//        Timer<MinecraftServer> scheduledEvents, CompoundTag customBossEvents, CompoundTag dragonFight,
//        LevelInfo levelInfo, GeneratorOptions generatorOptions, Lifecycle lifecycle_p, CallbackInfo ci
//    ) {
//        // TODO use more appropriate way to get rid of the warning screen
//        if (Global.enableAlternateDimensions && generatorOptions.getDimensions().getIds().size() == 8) {
//            lifecycle = Lifecycle.stable();
//        }
//    }
    
    @Inject(
        method = "updateProperties",
        at = @At("HEAD")
    )
    private void onUpdateProperties(
        DynamicRegistryManager dynamicRegistryManager, NbtCompound compoundTag,
        NbtCompound compoundTag2, CallbackInfo ci
    ) {
        ((IEGeneratorOptions) generatorOptions).setDimOptionRegistry(
            IPDimensionAPI.getAdditionalDimensionsRemoved(
                generatorOptions.getDimensions()
            )
        );
    }
}
