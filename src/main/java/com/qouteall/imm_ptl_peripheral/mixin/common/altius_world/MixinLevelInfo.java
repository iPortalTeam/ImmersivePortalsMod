package com.qouteall.imm_ptl_peripheral.mixin.common.altius_world;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.qouteall.imm_ptl_peripheral.altius_world.AltiusGameRule;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resource.DataPackSettings;
import net.minecraft.world.level.LevelInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelInfo.class)
public class MixinLevelInfo {
    
    @Inject(
        method = "fromDynamic",
        at = @At("RETURN"),
        cancellable = true
    )
    private static void onReadLevelInfoFromDynamic(
        Dynamic<?> dynamic,
        DataPackSettings dataPackSettings,
        CallbackInfoReturnable<LevelInfo> cir
    ) {
        DataResult<?> altiusElement = dynamic.getElement("altius");
        Object obj = altiusElement.get().left().orElse(null);
        if (obj != null) {
            if (obj instanceof CompoundTag) {
                AltiusGameRule.upgradeOldDimensionStack();
            }
        }
    }
}
