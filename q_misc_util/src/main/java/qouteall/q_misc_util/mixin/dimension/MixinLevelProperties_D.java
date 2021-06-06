package qouteall.q_misc_util.mixin.dimension;

import com.mojang.serialization.Lifecycle;
import qouteall.imm_ptl.core.ducks.IEGeneratorOptions;
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
import qouteall.q_misc_util.IPDimensionAPI;

@Mixin(LevelProperties.class)
public class MixinLevelProperties_D {
    @Shadow
    @Final
    private GeneratorOptions generatorOptions;
    
    @Shadow
    @Final
    @Mutable
    private Lifecycle lifecycle;
    
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
