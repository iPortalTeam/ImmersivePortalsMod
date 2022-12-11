package qouteall.q_misc_util.api;

import com.mojang.serialization.Lifecycle;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.core.DefaultedMappedRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.WorldOptions;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qouteall.q_misc_util.dimension.DimensionMisc;
import qouteall.q_misc_util.dimension.DynamicDimensionsImpl;
import qouteall.q_misc_util.dimension.ExtraDimensionStorage;
import qouteall.q_misc_util.mixin.dimension.IEMappedRegistry;

import java.util.Set;

public class DimensionAPI {
    private static final Logger logger = LogManager.getLogger();
    
    public static interface ServerDimensionsLoadCallback {
        /**
         * TODO update doc
         */
        void run(WorldOptions worldOptions, RegistryAccess registryAccess);
    }
    
    public static final Event<ServerDimensionsLoadCallback> serverDimensionsLoadEvent =
        EventFactory.createArrayBacked(
            ServerDimensionsLoadCallback.class,
            (listeners) -> ((worldOptions, registryManager) -> {
                Registry<LevelStem> levelStems = registryManager.registryOrThrow(Registries.LEVEL_STEM);
                for (ServerDimensionsLoadCallback listener : listeners) {
                    listener.run(worldOptions, registryManager);
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
        if (!(levelStemRegistry instanceof MappedRegistry<LevelStem> mapped)) {
            throw new RuntimeException("Failed to register the dimension");
        }
        
        if (!mapped.keySet().contains(dimensionId)) {
            // the vanilla freezing mechanism is used for validating dangling object references
            // for this API, that thing won't happen
            boolean oldIsFrozen = ((IEMappedRegistry) mapped).ip_getIsFrozen();
            ((IEMappedRegistry) mapped).ip_setIsFrozen(false);
            mapped.register(
                ResourceKey.create(Registries.LEVEL_STEM, dimensionId),
                levelStem,
                Lifecycle.stable()
            );
            ((IEMappedRegistry) mapped).ip_setIsFrozen(oldIsFrozen);
        }
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
