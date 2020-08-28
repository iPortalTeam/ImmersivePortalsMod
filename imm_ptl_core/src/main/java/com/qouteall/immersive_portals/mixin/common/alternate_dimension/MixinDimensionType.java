package com.qouteall.immersive_portals.mixin.common.alternate_dimension;

import com.mojang.serialization.Lifecycle;
import com.qouteall.immersive_portals.ModMain;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.util.registry.MutableRegistry;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.source.BiomeAccessType;
import net.minecraft.world.biome.source.HorizontalVoronoiBiomeAccessType;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.OptionalLong;

@Mixin(DimensionType.class)
public class MixinDimensionType {

    @Invoker("<init>")
    static DimensionType constructor(
        OptionalLong fixedTime, boolean hasSkylight, boolean hasCeiling, boolean ultrawarm,
        boolean natural, double coordinateScale, boolean hasEnderDragonFight, boolean piglinSafe,
        boolean bedWorks, boolean respawnAnchorWorks, boolean hasRaids, int logicalHeight,
        BiomeAccessType biomeAccessType,
        Identifier infiniburn, Identifier skyProperties, float ambientLight
    ) {
        return null;
    }
    
    @Inject(
        method = "addRegistryDefaults",
        at = @At("RETURN"),
        cancellable = true
    )
    private static void onAddRegistryDefaults(
        DynamicRegistryManager.Impl registryManager,
        CallbackInfoReturnable<DynamicRegistryManager.Impl> cir
    ) {
        MutableRegistry<DimensionType> mutableRegistry = registryManager.get(Registry.DIMENSION_TYPE_KEY);
        mutableRegistry.add(
            ModMain.surfaceType,
            ModMain.surfaceTypeObject,
            Lifecycle.stable()
        );
    }

    static {
        ModMain.surfaceTypeObject = constructor(
            OptionalLong.empty(), true, false,
            false, true, 1.0D, false,
            false, true, false, true,
            256, HorizontalVoronoiBiomeAccessType.INSTANCE,
            BlockTags.INFINIBURN_OVERWORLD.getId(),
            DimensionType.OVERWORLD_ID, 0.0F
        );
    }
}
