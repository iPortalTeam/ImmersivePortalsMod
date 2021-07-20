package qouteall.q_misc_util;

import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.SimpleRegistry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.MultiNoiseBiomeSource;
import net.minecraft.world.biome.source.TheEndBiomeSource;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qouteall.q_misc_util.api.DimensionAPI;
import qouteall.q_misc_util.mixin.dimension.DimensionTypeAccessor;

public class DimensionMisc {
    private static final Logger logger = LogManager.getLogger();
    
    public static boolean enableDedicatedServerEarlyReload = true;
    
    // fix the issue that nether and end get swallowed by DFU
    public static void addMissingVanillaDimensions(GeneratorOptions generatorOptions, DynamicRegistryManager registryManager) {
        SimpleRegistry<DimensionOptions> registry = generatorOptions.getDimensions();
        long seed = generatorOptions.getSeed();
        if (!registry.getIds().contains(DimensionOptions.NETHER.getValue())) {
            logger.error("Missing the nether. This may be caused by DFU. Trying to fix");
            
            DimensionAPI.addDimension(
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
            DimensionAPI.addDimension(
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
    
    public static void init() {
        DimensionAPI.serverDimensionsLoadEvent.register(DimensionMisc::addMissingVanillaDimensions);
    }
}
