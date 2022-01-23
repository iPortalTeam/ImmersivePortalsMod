package qouteall.imm_ptl.peripheral.mixin.common.altius_world;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.LevelSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import qouteall.imm_ptl.peripheral.altius_world.AltiusGameRule;

@Mixin(LevelSettings.class)
public class MixinLevelInfo {
    
    @Inject(
        method = "Lnet/minecraft/world/level/LevelSettings;parse(Lcom/mojang/serialization/Dynamic;Lnet/minecraft/world/level/DataPackConfig;)Lnet/minecraft/world/level/LevelSettings;",
        at = @At("RETURN"),
        cancellable = true
    )
    private static void onReadLevelInfoFromDynamic(
        Dynamic<?> dynamic,
        DataPackConfig dataPackSettings,
        CallbackInfoReturnable<LevelSettings> cir
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
