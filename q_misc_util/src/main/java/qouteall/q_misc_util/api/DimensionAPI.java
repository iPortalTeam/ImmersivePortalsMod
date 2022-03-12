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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qouteall.q_misc_util.MiscHelper;
import qouteall.q_misc_util.dimension.DimId;
import qouteall.q_misc_util.dimension.DimensionMisc;
import qouteall.q_misc_util.dimension.DynamicDimensionsImpl;
import qouteall.q_misc_util.dimension.ExtraDimensionStorage;

import java.util.Set;
import java.util.function.Consumer;

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
    
    /**
     * Add a new dimension during server initialization.
     * The added dimension won't be saved into `level.dat`.
     * Cannot be used when the server is running.
     */
    public static void addDimension(
        Registry<LevelStem> levelStemRegistry,
        ResourceLocation dimensionId,
        Holder<DimensionType> dimensionTypeHolder,
        ChunkGenerator chunkGenerator
    ) {
        addDimension(levelStemRegistry, dimensionId, new LevelStem(
            dimensionTypeHolder,
            chunkGenerator
        ));
    }
    
    /**
     * Add a new dimension during server initialization.
     * The added dimension won't be saved into `level.dat`.
     * Cannot be used when the server is running.
     */
    private static void addDimension(
        Registry<LevelStem> levelStemRegistry,
        ResourceLocation dimensionId,
        LevelStem levelStem
    ) {
        if (levelStemRegistry instanceof MappedRegistry<LevelStem> mapped) {
            if (!mapped.keySet().contains(dimensionId)) {
                
                mapped.register(
                    ResourceKey.create(Registry.LEVEL_STEM_REGISTRY, dimensionId),
                    levelStem,
                    Lifecycle.stable()
                );
            }
        }
        else {
            throw new RuntimeException("Failed to register the dimension");
        }
        
        markDimensionNonPersistent(dimensionId);
    }
    
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
     * Don't use this for dynamically-added dimensions
     * <p>
     * If you don't mark a dimension non-persistent, then it will be saved into "level.dat" file
     * Then when you upgrade the world or remove the mod, DFU cannot recognize it
     * then the nether and the end will vanish.
     * It's recommended to mark your own dimension non-persistent
     */
    public static void markDimensionNonPersistent(ResourceLocation dimensionId) {
        DimensionMisc.nonPersistentDimensions.add(dimensionId);
    }
    
    /**
     * Add a new dimension when the server is running
     * Cannot be used during server initialization
     */
    public static void addDimensionDynamically(
        ResourceLocation dimensionId,
        LevelStem levelStem
    ) {
        DynamicDimensionsImpl.addDimensionDynamically(dimensionId, levelStem);
    }
    
    public static void removeDimensionDynamically(ServerLevel world) {
        DynamicDimensionsImpl.removeDimensionDynamically(world);
    }
    
    /**
     * Extra dimensions are stored as json files in folder `q_extra_dimensions` in the world saving
     * We don't store dimensions in `level.dat` because if you uninstall the mod,
     * DFU will not be able to recognize the chunk generator and cause world data loss (nether and end will vanish)
     */
    public static void saveDimensionIntoExtraStorage(ResourceKey<Level> dimension) {
        ExtraDimensionStorage.saveDimensionIntoExtraStorage(dimension);
    }
    
    /**
     * Delete the json file in the extra dimension storage
     */
    public static boolean removeDimensionFromExtraStorage(ResourceKey<Level> dimension) {
        return ExtraDimensionStorage.removeDimensionFromExtraStorage(dimension);
    }
    
    public static interface DynamicDimensionUpdateListener {
        void run(Set<ResourceKey<Level>> dimensions);
    }
    
    public static final Event<DynamicDimensionUpdateListener> serverDimensionDynamicUpdateEvent =
        EventFactory.createArrayBacked(
            DynamicDimensionUpdateListener.class,
            arr -> (set) -> {
                for (DynamicDimensionUpdateListener runnable : arr) {
                    runnable.run(set);
                }
            }
        );
    
    public static final Event<DynamicDimensionUpdateListener> clientDimensionDynamicUpdateEvent =
        EventFactory.createArrayBacked(
            DynamicDimensionUpdateListener.class,
            arr -> (set) -> {
                for (DynamicDimensionUpdateListener runnable : arr) {
                    runnable.run(set);
                }
            }
        );
}
