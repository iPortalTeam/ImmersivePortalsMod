package com.qouteall.immersive_portals.mixin.altius_world;

import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Lifecycle;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.altius_world.AltiusInfo;
import com.qouteall.immersive_portals.ducks.IELevelProperties;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.level.LevelInfo;
import net.minecraft.world.level.LevelProperties;
import net.minecraft.world.timer.Timer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.LinkedHashSet;
import java.util.UUID;

@Mixin(LevelProperties.class)
public class MixinLevelProperties implements IELevelProperties {
    AltiusInfo altiusInfo;
    
    @Inject(
        method = "<init>(Lcom/mojang/datafixers/DataFixer;ILnet/minecraft/nbt/CompoundTag;ZIIIJJIIIZIZZZLnet/minecraft/world/border/WorldBorder$Properties;IILjava/util/UUID;Ljava/util/LinkedHashSet;Lnet/minecraft/world/timer/Timer;Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/world/level/LevelInfo;Lnet/minecraft/world/gen/GeneratorOptions;Lcom/mojang/serialization/Lifecycle;)V",
        at = @At("RETURN")
    )
    private void onConstructedFromLevelInfo(
        DataFixer dataFixer,
        int dataVersion,
        CompoundTag playerData,
        boolean modded,
        int spawnX,
        int spawnY,
        int spawnZ,
        long time,
        long timeOfDay,
        int version,
        int clearWeatherTime,
        int rainTime,
        boolean raining,
        int thunderTime,
        boolean thundering,
        boolean initialized,
        boolean difficultyLocked,
        WorldBorder.Properties worldBorder,
        int wanderingTraderSpawnDelay,
        int wanderingTraderSpawnChance,
        UUID wanderingTraderId,
        LinkedHashSet<String> serverBrands,
        Timer<MinecraftServer> timer,
        CompoundTag compoundTag,
        CompoundTag compoundTag2,
        LevelInfo levelInfo,
        GeneratorOptions generatorOptions,
        Lifecycle lifecycle,
        CallbackInfo ci
    ) {
        altiusInfo = ((IELevelProperties) (Object) levelInfo).getAltiusInfo();
    }
    
//    @Inject(
//        method = "<init>(Lnet/minecraft/nbt/CompoundTag;Lcom/mojang/datafixers/DataFixer;ILnet/minecraft/nbt/CompoundTag;)V",
//        at = @At("RETURN")
//    )
//    private void onConstructedFromTag(
//        CompoundTag compoundTag,
//        DataFixer dataFixer,
//        int i,
//        CompoundTag compoundTag2,
//        CallbackInfo ci
//    ) {
//        if (compoundTag.contains("altius")) {
//            Tag tag = compoundTag.get("altius");
//            altiusInfo = AltiusInfo.fromTag(((CompoundTag) tag));
//        }
//        if (compoundTag.contains("generatorName", 8)) {
//            String generatorName = compoundTag.getString("generatorName");
//            if (generatorName.equals("imm_ptl_altius")) {
//                Helper.log("Upgraded old altius world to new altius world");
//                altiusInfo = AltiusInfo.getDummy();
//            }
//        }
//    }
    
//    @Inject(
//        method = "updateProperties",
//        at = @At("RETURN")
//    )
//    private void onUpdateProperties(CompoundTag levelTag, CompoundTag playerTag, CallbackInfo ci) {
//        if (altiusInfo != null) {
//            levelTag.put("altius", altiusInfo.toTag());
//        }
//    }
    
    @Override
    public AltiusInfo getAltiusInfo() {
        return altiusInfo;
    }
    
    @Override
    public void setAltiusInfo(AltiusInfo cond) {
        altiusInfo = cond;
    }
}
