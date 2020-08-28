package com.qouteall.imm_ptl_peripheral.mixin.common.altius_world;

import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.Lifecycle;
import com.qouteall.imm_ptl_peripheral.alternate_dimension.AlternateDimensions;
import com.qouteall.imm_ptl_peripheral.altius_world.AltiusInfo;
import com.qouteall.imm_ptl_peripheral.ducks.IELevelProperties;
import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.ducks.IEGeneratorOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.level.LevelInfo;
import net.minecraft.world.level.LevelProperties;
import net.minecraft.world.level.storage.SaveVersionInfo;
import net.minecraft.world.timer.Timer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.LinkedHashSet;
import java.util.UUID;

@Mixin(LevelProperties.class)
public class MixinLevelProperties implements IELevelProperties {
    
    @Shadow
    @Final
    private Lifecycle lifecycle;
    @Shadow
    @Final
    private GeneratorOptions generatorOptions;
    AltiusInfo altiusInfo;
    
    @Inject(
        method = "<init>(Lcom/mojang/datafixers/DataFixer;ILnet/minecraft/nbt/CompoundTag;ZIIIFJJIIIZIZZZLnet/minecraft/world/border/WorldBorder$Properties;IILjava/util/UUID;Ljava/util/LinkedHashSet;Lnet/minecraft/world/timer/Timer;Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/world/level/LevelInfo;Lnet/minecraft/world/gen/GeneratorOptions;Lcom/mojang/serialization/Lifecycle;)V",
        at = @At("RETURN")
    )
    private void onConstructedFromLevelInfo(
        DataFixer dataFixer, int dataVersion, CompoundTag playerData,
        boolean modded, int spawnX, int spawnY, int spawnZ, float spawnAngle,
        long time, long timeOfDay, int version, int clearWeatherTime, int rainTime,
        boolean raining, int thunderTime, boolean thundering, boolean initialized,
        boolean difficultyLocked, WorldBorder.Properties worldBorder, int wanderingTraderSpawnDelay,
        int wanderingTraderSpawnChance, UUID wanderingTraderId, LinkedHashSet<String> serverBrands,
        Timer<MinecraftServer> scheduledEvents, CompoundTag customBossEvents, CompoundTag dragonFight,
        LevelInfo levelInfo, GeneratorOptions generatorOptions, Lifecycle lifecycle, CallbackInfo ci
    ) {
        altiusInfo = ((IELevelProperties) (Object) levelInfo).getAltiusInfo();
        
        // TODO use more appropriate way to get rid of the warning screen
        if (Global.enableAlternateDimensions && generatorOptions.getDimensions().getIds().size() == 8) {
            lifecycle = Lifecycle.stable();
        }
    }
    
    @Inject(
        method = "method_29029",
        at = @At("RETURN"),
        cancellable = true
    )
    private static void onReadDataFromTag(
        Dynamic<Tag> dynamic,
        DataFixer dataFixer,
        int i,
        CompoundTag playerTag,
        LevelInfo levelInfo,
        SaveVersionInfo saveVersionInfo,
        GeneratorOptions generatorOptions,
        Lifecycle lifecycle,
        CallbackInfoReturnable<LevelProperties> cir
    ) {
        LevelProperties levelProperties = cir.getReturnValue();
        
        MixinLevelProperties this_ = (MixinLevelProperties) (Object) levelProperties;
        
        AltiusInfo levelInfoAltiusInfo = ((IELevelProperties) (Object) levelInfo).getAltiusInfo();
        if (levelInfoAltiusInfo != null) {
            this_.altiusInfo = levelInfoAltiusInfo;
            return;
        }
        
        Tag altiusTag = dynamic.getElement("altius", null);
        if (altiusTag != null) {
            this_.altiusInfo = AltiusInfo.fromTag(((CompoundTag) altiusTag));
        }
    }
    
    @Inject(
        method = "updateProperties",
        at = @At("HEAD")
    )
    private void onUpdateProperties(
        DynamicRegistryManager dynamicRegistryManager, CompoundTag compoundTag,
        CompoundTag compoundTag2, CallbackInfo ci
    ) {
        ((IEGeneratorOptions) generatorOptions).setDimOptionRegistry(
            AlternateDimensions.getAlternateDimensionsRemoved(
                generatorOptions.getDimensions()
            )
        );
        if (altiusInfo != null) {
            compoundTag.put("altius", altiusInfo.toTag());
        }
    }
    
    @Override
    public AltiusInfo getAltiusInfo() {
        return altiusInfo;
    }
    
    @Override
    public void setAltiusInfo(AltiusInfo cond) {
        altiusInfo = cond;
    }
}
