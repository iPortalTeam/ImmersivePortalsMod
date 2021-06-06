package qouteall.q_misc_util;

import com.mojang.serialization.Lifecycle;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.MultiNoiseBiomeSource;
import net.minecraft.world.biome.source.TheEndBiomeSource;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.util.registry.SimpleRegistry;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import qouteall.q_misc_util.mixin.dimension.DimensionTypeAccessor;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

public class IPDimensionAPI {
    private static final Logger logger = LogManager.getLogger();
    
    public static interface ServerDimensionsLoadCallback {
        void run(GeneratorOptions generatorOptions, DynamicRegistryManager registryManager);
    }
    
    public static final Event<ServerDimensionsLoadCallback> serverDimensionsLoadEvent =
        EventFactory.createArrayBacked(
            ServerDimensionsLoadCallback.class,
            (listeners) -> ((generatorOptions, registryManager) -> {
                for (ServerDimensionsLoadCallback listener : listeners) {
                    listener.run(generatorOptions, registryManager);
                }
            })
        );
    
    private static final Set<Identifier> nonPersistentDimensions = new HashSet<>();
    
    public static void init() {
        serverDimensionsLoadEvent.register(IPDimensionAPI::addMissingVanillaDimensions);
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
                RegistryKey.of(Registry.DIMENSION_KEY, dimensionId),
                new DimensionOptions(
                    dimensionTypeSupplier,
                    chunkGenerator
                ),
                Lifecycle.experimental()
            );
        }
    }
    
    public static void markDimensionNonPersistent(Identifier dimensionId) {
        nonPersistentDimensions.add(dimensionId);
    }
    
    // This is not API
    // When DFU does not recognize a mod dimension (in level.dat) it will throw an error
    // then the nether and the end will be swallowed (https://github.com/TelepathicGrunt/Bumblezone-Fabric/issues/20)
    // to fix that, don't store the custom dimensions into level.dat
    public static SimpleRegistry<DimensionOptions> getAdditionalDimensionsRemoved(
        SimpleRegistry<DimensionOptions> registry
    ) {
        if (nonPersistentDimensions.isEmpty()) {
            return registry;
        }
        
        return MiscHelper.filterAndCopyRegistry(
            registry,
            (key, obj) -> {
                Identifier identifier = key.getValue();
                return !nonPersistentDimensions.contains(identifier);
            }
        );
    }
    
    // fix the issue that nether and end get swallowed by DFU
    private static void addMissingVanillaDimensions(GeneratorOptions generatorOptions, DynamicRegistryManager registryManager) {
        SimpleRegistry<DimensionOptions> registry = generatorOptions.getDimensions();
        long seed = generatorOptions.getSeed();
        if (!registry.getIds().contains(DimensionOptions.NETHER.getValue())) {
            logger.error("Missing the nether. This may be caused by DFU. Trying to fix");
            
            IPDimensionAPI.addDimension(
                seed,
                registry,
                DimensionOptions.NETHER.getValue(),
                () -> DimensionTypeAccessor._getTheNether(),
                createNetherGenerator(
                    registryManager.get(Registry.BIOME_KEY),
                    registryManager.get(Registry.CHUNK_GENERATOR_SETTINGS_KEY),
                    seed
                )
            );
        }
        
        if (!registry.getIds().contains(DimensionOptions.END.getValue())) {
            logger.error("Missing the end. This may be caused by DFU. Trying to fix");
            IPDimensionAPI.addDimension(
                seed,
                registry,
                DimensionOptions.END.getValue(),
                () -> DimensionTypeAccessor._getTheEnd(),
                createEndGenerator(
                    registryManager.get(Registry.BIOME_KEY),
                    registryManager.get(Registry.CHUNK_GENERATOR_SETTINGS_KEY),
                    seed
                )
            );
        }
    }
    
    /**
     * Copied from {@link DimensionType}
     */
    private static ChunkGenerator createNetherGenerator(Registry<Biome> biomeRegistry, Registry<ChunkGeneratorSettings> chunkGeneratorSettingsRegistry, long seed) {
        return new NoiseChunkGenerator(MultiNoiseBiomeSource.Preset.NETHER.getBiomeSource(biomeRegistry, seed), seed, () -> {
            return (ChunkGeneratorSettings) chunkGeneratorSettingsRegistry.getOrThrow(ChunkGeneratorSettings.NETHER);
        });
    }
    
    private static ChunkGenerator createEndGenerator(Registry<Biome> biomeRegistry, Registry<ChunkGeneratorSettings> chunkGeneratorSettingsRegistry, long seed) {
        return new NoiseChunkGenerator(new TheEndBiomeSource(biomeRegistry, seed), seed, () -> {
            return (ChunkGeneratorSettings) chunkGeneratorSettingsRegistry.getOrThrow(ChunkGeneratorSettings.END);
        });
    }
}
