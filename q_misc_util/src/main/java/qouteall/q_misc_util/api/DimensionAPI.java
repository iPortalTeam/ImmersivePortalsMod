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
import qouteall.q_misc_util.dimension.DimensionMisc;
import qouteall.q_misc_util.dimension.DynamicDimensionsImpl;
import qouteall.q_misc_util.dimension.ExtraDimensionStorage;

import java.util.Set;

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
     * It's recommended to save the dimension config using {@link DimensionAPI#saveDimensionConfiguration(ResourceKey)} ,
     * otherwise that dimension will be lost when you restart the server
     */
    public static void addDimensionDynamically(
        ResourceLocation dimensionId,
        LevelStem levelStem
    ) {
        DynamicDimensionsImpl.addDimensionDynamically(dimensionId, levelStem);
    }
    
    /**
     * Remove a dimension dynamically.
     * Cannot be used during server initialization. Cannot remove vanilla dimensions.
     * Does not delete that dimension's world saving files.
     */
    public static void removeDimensionDynamically(ServerLevel world) {
        DynamicDimensionsImpl.removeDimensionDynamically(world);
    }
    
    /**
     * Store the dimension configurations as a json file in folder `q_dimension_configs` in the world saving
     * We don't store dimensions in `level.dat` because if you uninstall the mod,
     * DFU will not be able to recognize the chunk generator and cause world data loss (nether and end will vanish)
     */
    public static void saveDimensionConfiguration(ResourceKey<Level> dimension) {
        Validate.isTrue(
            !dimension.location().getNamespace().equals("minecraft"),
            "cannot save a vanilla dimension"
        );
        ExtraDimensionStorage.saveDimensionIntoExtraStorage(dimension);
    }
    
    /**
     * Delete the dimension configuration json file from folder `q_dimension_configs`
     *
     * @return True if it finds the file and deleted it successfully
     */
    public static boolean deleteDimensionConfiguration(ResourceKey<Level> dimension) {
        return ExtraDimensionStorage.removeDimensionFromExtraStorage(dimension);
    }
    
    public static interface DynamicUpdateListener {
        void run(Set<ResourceKey<Level>> dimensions);
    }
    
    /**
     * Will be triggered when the server dynamically add or remove a dimension
     * Does not get triggered during server initialization
     */
    public static final Event<DynamicUpdateListener> serverDimensionDynamicUpdateEvent =
        EventFactory.createArrayBacked(
            DynamicUpdateListener.class,
            arr -> (set) -> {
                for (DynamicUpdateListener runnable : arr) {
                    runnable.run(set);
                }
            }
        );
    
    /**
     * Will be triggered when the client receives dimension data synchronization
     */
    public static final Event<DynamicUpdateListener> clientDimensionUpdateEvent =
        EventFactory.createArrayBacked(
            DynamicUpdateListener.class,
            arr -> (set) -> {
                for (DynamicUpdateListener runnable : arr) {
                    runnable.run(set);
                }
            }
        );
}
