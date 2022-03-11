package qouteall.q_misc_util.api;

import com.mojang.serialization.Lifecycle;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qouteall.q_misc_util.DimensionMisc;

public class DimensionAPI {
    private static final Logger logger = LogManager.getLogger();
    
    public static interface ServerDimensionsLoadCallback {
        /**
         * You can get the registry of dimensions using `worldGenSettings.dimensions()`
         * For biomes and dimension types, you can get them from the registry access
         */
        void run(WorldGenSettings worldGenSettings, RegistryAccess registryAccess);
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
    
    public static void addDimension(
        Registry<LevelStem> levelStemRegistry,
        ResourceLocation dimensionId,
        Holder<DimensionType> dimensionTypeHolder,
        ChunkGenerator chunkGenerator
    ) {
        if (levelStemRegistry instanceof MappedRegistry<LevelStem> mapped) {
            if (!mapped.keySet().contains(dimensionId)) {
                mapped.register(
                    ResourceKey.create(Registry.LEVEL_STEM_REGISTRY, dimensionId),
                    new LevelStem(
                        dimensionTypeHolder,
                        chunkGenerator
                    ),
                    Lifecycle.experimental()
                );
            }
        }
        else {
            throw new RuntimeException("Failed to register the dimension");
        }
    }
    
    // The "seed" argument is not needed. Use the above one.
    @Deprecated
    public static void addDimension(
        long argSeed,
        Registry<LevelStem> dimensionOptionsRegistry,
        ResourceLocation dimensionId,
        Holder<DimensionType> dimensionTypeHolder,
        ChunkGenerator chunkGenerator
    ) {
        addDimension(dimensionOptionsRegistry, dimensionId, dimensionTypeHolder, chunkGenerator);
    }
    
    
    /**
     * If you don't mark a dimension non-persistent, then it will be saved into "level.dat" file
     * Then when you upgrade the world or remove the mod, DFU cannot recognize it
     *  then the nether and the end will vanish.
     * It's recommended to mark your own dimension non-persistent
     */
    public static void markDimensionNonPersistent(ResourceLocation dimensionId) {
        DimensionMisc.nonPersistentDimensions.add(dimensionId);
    }
    
}
