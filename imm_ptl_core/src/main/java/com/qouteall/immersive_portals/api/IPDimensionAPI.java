package com.qouteall.immersive_portals.api;

import com.mojang.serialization.Lifecycle;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.my_util.SignalBiArged;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.util.registry.SimpleRegistry;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.gen.chunk.ChunkGenerator;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

public class IPDimensionAPI {
    public static final SignalBiArged<GeneratorOptions, DynamicRegistryManager> onServerWorldInit = new SignalBiArged<>();
    
    public static final Set<Identifier> dimensionsExcludedFromBeingSaved = new HashSet<>();
    
    public static void init() {
        onServerWorldInit.connect(IPDimensionAPI::addMissingVanillaDimensions);
    }
    
    public static void addDimension(
        long argSeed,
        SimpleRegistry<DimensionOptions> dimensionOptionsRegistry,
        Identifier dimensionId,
        Supplier<DimensionType> dimensionTypeSupplier,
        ChunkGenerator chunkGenerator
    ) {
        if (!dimensionOptionsRegistry.getIds().contains(dimensionId)) {
            dimensionOptionsRegistry.add(
                RegistryKey.of(Registry.DIMENSION_OPTIONS, dimensionId),
                new DimensionOptions(
                    dimensionTypeSupplier,
                    chunkGenerator
                ),
                Lifecycle.experimental()
            );
        }
    }
    
    public static void markDimensionNonPersistent(Identifier dimensionId) {
        dimensionsExcludedFromBeingSaved.add(dimensionId);
    }
    
    // don't store dimension info into level.dat
    // avoid weird dfu error
    public static SimpleRegistry<DimensionOptions> getAdditionalDimensionsRemoved(
        SimpleRegistry<DimensionOptions> registry
    ) {
        return McHelper.filterAndCopyRegistry(
            registry,
            (key, obj) -> {
                Identifier identifier = key.getValue();
                return !dimensionsExcludedFromBeingSaved.contains(identifier);
            }
        );
    }
    
    private static void addMissingVanillaDimensions(GeneratorOptions generatorOptions, DynamicRegistryManager registryManager) {
        SimpleRegistry<DimensionOptions> registry = generatorOptions.getDimensions();
        long seed = generatorOptions.getSeed();
        if (!registry.getIds().contains(DimensionOptions.NETHER.getValue())) {
            Helper.err("Missing the nether. This may be caused by DFU. Trying to fix");
            
            IPDimensionAPI.addDimension(
                seed,
                registry,
                DimensionOptions.NETHER.getValue(),
                () -> DimensionType.THE_NETHER,
                DimensionType.createNetherGenerator(
                    registryManager.get(Registry.BIOME_KEY),
                    registryManager.get(Registry.NOISE_SETTINGS_WORLDGEN),
                    seed
                )
            );
        }
        
        if (!registry.getIds().contains(DimensionOptions.END.getValue())) {
            Helper.err("Missing the end. This may be caused by DFU. Trying to fix");
            IPDimensionAPI.addDimension(
                seed,
                registry,
                DimensionOptions.END.getValue(),
                () -> DimensionType.THE_END,
                DimensionType.createEndGenerator(
                    registryManager.get(Registry.BIOME_KEY),
                    registryManager.get(Registry.NOISE_SETTINGS_WORLDGEN),
                    seed
                )
            );
        }
    }
}
