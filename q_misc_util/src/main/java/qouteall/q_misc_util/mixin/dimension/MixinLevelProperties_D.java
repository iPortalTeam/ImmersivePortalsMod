package qouteall.q_misc_util.mixin.dimension;

import com.mojang.serialization.Lifecycle;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.storage.PrimaryLevelData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.q_misc_util.api.DimensionAPI;
import qouteall.q_misc_util.ducks.IEGeneratorOptions;

@Mixin(PrimaryLevelData.class)
public class MixinLevelProperties_D {
    @Shadow
    @Final
    private WorldGenSettings worldGenSettings;
    
    @Shadow
    @Final
    @Mutable
    private Lifecycle worldGenSettingsLifecycle;
    
    @Inject(
        method = "Lnet/minecraft/world/level/storage/PrimaryLevelData;setTagData(Lnet/minecraft/core/RegistryAccess;Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/nbt/CompoundTag;)V",
        at = @At("HEAD")
    )
    private void onUpdateProperties(
        RegistryAccess dynamicRegistryManager, CompoundTag compoundTag,
        CompoundTag compoundTag2, CallbackInfo ci
    ) {
        ((IEGeneratorOptions) worldGenSettings).setDimOptionRegistry(
            DimensionAPI._getAdditionalDimensionsRemoved(
                worldGenSettings.dimensions()
            )
        );
    }
}
