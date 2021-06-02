package com.qouteall.imm_ptl_peripheral.mixin.common.altius_world;

import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.Lifecycle;
import com.qouteall.imm_ptl_peripheral.altius_world.AltiusGameRule;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.level.LevelInfo;
import net.minecraft.world.level.LevelProperties;
import net.minecraft.world.level.storage.SaveVersionInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelProperties.class)
public class MixinLevelProperties {
    
   
    
    @Shadow
    @Final
    private GeneratorOptions generatorOptions;
    
    @Inject(
        method = "readProperties",
        at = @At("RETURN"),
        cancellable = true
    )
    private static void onReadDataFromTag(
        Dynamic<NbtElement> dynamic,
        DataFixer dataFixer,
        int i,
        NbtCompound playerTag,
        LevelInfo levelInfo,
        SaveVersionInfo saveVersionInfo,
        GeneratorOptions generatorOptions,
        Lifecycle lifecycle,
        CallbackInfoReturnable<LevelProperties> cir
    ) {
        LevelProperties levelProperties = cir.getReturnValue();
        
        MixinLevelProperties this_ = (MixinLevelProperties) (Object) levelProperties;
        
        NbtElement altiusTag = dynamic.getElement("altius", null);
        if (altiusTag != null) {
            AltiusGameRule.upgradeOldDimensionStack();
        }
    }
    
   
}
