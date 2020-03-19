package com.qouteall.immersive_portals.mixin.altius_world;

import com.mojang.datafixers.DataFixer;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.altius_world.AltiusInfo;
import com.qouteall.immersive_portals.ducks.IELevelProperties;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.LevelInfo;
import net.minecraft.world.level.LevelProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelProperties.class)
public class MixinLevelProperties implements IELevelProperties {
    AltiusInfo altiusInfo;
    
    @Inject(
        method = "<init>(Lnet/minecraft/world/level/LevelInfo;Ljava/lang/String;)V",
        at = @At("RETURN")
    )
    private void onConstructedFromLevelInfo(
        LevelInfo levelInfo, String levelName, CallbackInfo ci
    ) {
        altiusInfo = ((IELevelProperties) (Object) levelInfo).getAltiusInfo();
    }
    
    @Inject(
        method = "<init>(Lnet/minecraft/nbt/CompoundTag;Lcom/mojang/datafixers/DataFixer;ILnet/minecraft/nbt/CompoundTag;)V",
        at = @At("RETURN")
    )
    private void onConstructedFromTag(
        CompoundTag compoundTag,
        DataFixer dataFixer,
        int i,
        CompoundTag compoundTag2,
        CallbackInfo ci
    ) {
        if (compoundTag.contains("altius")) {
            Tag tag = compoundTag.get("altius");
            altiusInfo = AltiusInfo.fromTag(((CompoundTag) tag));
        }
        if (compoundTag.contains("generatorName", 8)) {
            String generatorName = compoundTag.getString("generatorName");
            if (generatorName.equals("imm_ptl_altius")) {
                Helper.log("Upgraded old altius world to new altius world");
                altiusInfo = AltiusInfo.getDummy();
            }
        }
    }
    
    @Inject(
        method = "updateProperties",
        at = @At("RETURN")
    )
    private void onUpdateProperties(CompoundTag levelTag, CompoundTag playerTag, CallbackInfo ci) {
        if (altiusInfo != null) {
            levelTag.put("altius", altiusInfo.toTag());
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
