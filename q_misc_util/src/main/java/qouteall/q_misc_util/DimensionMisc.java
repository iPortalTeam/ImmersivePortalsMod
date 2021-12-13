package qouteall.q_misc_util;

import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.world.gen.GeneratorOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qouteall.q_misc_util.api.DimensionAPI;

public class DimensionMisc {
    private static final Logger logger = LogManager.getLogger();
    
    // fix the issue that nether and end get swallowed by DFU
    public static void addMissingVanillaDimensions(GeneratorOptions generatorOptions, DynamicRegistryManager registryManager) {
//        SimpleRegistry<DimensionOptions> registry = generatorOptions.getDimensions();
//        long seed = generatorOptions.getSeed();
//        if (!registry.getIds().contains(DimensionOptions.NETHER.getValue())) {
//            logger.error("Missing the nether. This may be caused by DFU. Trying to fix");
//
//            DimensionAPI.addDimension(
//                seed,
//                registry,
//                DimensionOptions.NETHER.getValue(),
//                () -> DimensionTypeAccessor._getTheNether(),
//                createNetherGenerator(
//                    registryManager.get(Registry.BIOME_KEY),
//                    registryManager.get(Registry.CHUNK_GENERATOR_SETTINGS_KEY),
//                    seed
//                )
//            );
//        }
//
//        if (!registry.getIds().contains(DimensionOptions.END.getValue())) {
//            logger.error("Missing the end. This may be caused by DFU. Trying to fix");
//            DimensionAPI.addDimension(
//                seed,
//                registry,
//                DimensionOptions.END.getValue(),
//                () -> DimensionTypeAccessor._getTheEnd(),
//                createEndGenerator(
//                    registryManager.get(Registry.BIOME_KEY),
//                    registryManager.get(Registry.CHUNK_GENERATOR_SETTINGS_KEY),
//                    seed
//                )
//            );
//        }
    }
    
//    /**
//     * Copied from {@link DimensionType}
//     */
//    private static ChunkGenerator createNetherGenerator(Registry<Biome> biomeRegistry, Registry<ChunkGeneratorSettings> chunkGeneratorSettingsRegistry, long seed) {
//        return new NoiseChunkGenerator(MultiNoiseBiomeSource.Preset.NETHER.getBiomeSource(biomeRegistry, seed), seed, () -> {
//            return (ChunkGeneratorSettings) chunkGeneratorSettingsRegistry.getOrThrow(ChunkGeneratorSettings.NETHER);
//        });
//    }
//
//    private static ChunkGenerator createEndGenerator(Registry<Biome> biomeRegistry, Registry<ChunkGeneratorSettings> chunkGeneratorSettingsRegistry, long seed) {
//        return new NoiseChunkGenerator(new TheEndBiomeSource(biomeRegistry, seed), seed, () -> {
//            return (ChunkGeneratorSettings) chunkGeneratorSettingsRegistry.getOrThrow(ChunkGeneratorSettings.END);
//        });
//    }
    
    public static void init() {
        DimensionAPI.serverDimensionsLoadEvent.register(DimensionMisc::addMissingVanillaDimensions);
    }
}
