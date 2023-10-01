package qouteall.q_misc_util.api;

import com.mojang.serialization.Lifecycle;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qouteall.q_misc_util.dimension.DynamicDimensionsImpl;

import java.util.Set;

public class DimensionAPI {
    private static final Logger LOGGER = LogManager.getLogger();
    
    public static interface ServerDimensionsLoadCallback {
        /**
         * TODO update doc
         */
        void run(
            MinecraftServer server,
            WorldOptions worldOptions,
            MappedRegistry<LevelStem> levelStemRegistry,
            RegistryAccess registryAccess
        );
    }
    
    /**
     * This event is fired when loading custom dimensions when the server is starting.
     * Note: when showing the dimension list on client dimension stack menu, this event will also be fired.
     */
    public static final Event<ServerDimensionsLoadCallback> SEVER_DIMENSIONS_LOAD_EVENT =
        EventFactory.createArrayBacked(
            ServerDimensionsLoadCallback.class,
            (listeners) -> ((server, worldOptions, levelStemRegistry, registryAccess) -> {
                for (ServerDimensionsLoadCallback listener : listeners) {
                    try {
                        listener.run(server, worldOptions, levelStemRegistry, registryAccess);
                    }
                    catch (Exception e) {
                        LOGGER.error("Error registering dimension", e);
                    }
                }
            })
        );
    
    /**
     * A helper method for adding dimension into registry.
     * Should only be used in {@link DimensionAPI#SEVER_DIMENSIONS_LOAD_EVENT}
     */
    public static void addDimensionToRegistry(
        MappedRegistry<LevelStem> levelStemRegistry,
        ResourceLocation dimensionId,
        Holder<DimensionType> dimensionTypeHolder,
        ChunkGenerator chunkGenerator
    ) {
        addDimensionToRegistry(
            levelStemRegistry, dimensionId,
            new LevelStem(dimensionTypeHolder, chunkGenerator)
        );
    }
    
    /**
     * A helper method for adding dimension into registry.
     * Should only be used in {@link DimensionAPI#SEVER_DIMENSIONS_LOAD_EVENT}
     */
    private static void addDimensionToRegistry(
        MappedRegistry<LevelStem> levelStemRegistry,
        ResourceLocation dimensionId,
        LevelStem levelStem
    ) {
        if (!levelStemRegistry.containsKey(dimensionId)) {
            levelStemRegistry.register(
                ResourceKey.create(Registries.LEVEL_STEM, dimensionId),
                levelStem,
                Lifecycle.stable()
            );
        }
    }
    
    /**
     * Add a new dimension when the server is running.
     * Cannot be used during server initialization.
     * The new dimension's config will be saved in the `level.dat` file.
     */
    public static void addDimensionDynamically(
        MinecraftServer server,
        ResourceLocation dimensionId,
        LevelStem levelStem
    ) {
        DynamicDimensionsImpl.addDimensionDynamically(server, dimensionId, levelStem);
    }
    
    /**
     * Remove a dimension dynamically.
     * Cannot be used during server initialization.
     * Cannot remove vanilla dimensions.
     * The removed dimension's config will be removed from the `level.dat` file.
     * Does not delete that dimension's world saving files.
     */
    public static void removeDimensionDynamically(ServerLevel world) {
        DynamicDimensionsImpl.removeDimensionDynamically(world);
    }
    
    public static interface ServerDynamicUpdateListener {
        void run(MinecraftServer server, Set<ResourceKey<Level>> dimensions);
    }
    
    public static interface ClientDynamicUpdateListener {
        void run(Set<ResourceKey<Level>> dimensions);
    }
    
    /**
     * Will be triggered when the server dynamically add or remove a dimension.
     * Does not trigger during server initialization.
     */
    public static final Event<ServerDynamicUpdateListener> SERVER_DIMENSION_DYNAMIC_UPDATE_EVENT =
        EventFactory.createArrayBacked(
            ServerDynamicUpdateListener.class,
            arr -> (server, dims) -> {
                for (ServerDynamicUpdateListener runnable : arr) {
                    runnable.run(server, dims);
                }
            }
        );
    
    /**
     * Will be triggered when the client receives dimension data synchronization
     */
    public static final Event<ClientDynamicUpdateListener> CLIENT_DIMENSION_UPDATE_EVENT =
        EventFactory.createArrayBacked(
            ClientDynamicUpdateListener.class,
            arr -> (set) -> {
                for (ClientDynamicUpdateListener runnable : arr) {
                    runnable.run(set);
                }
            }
        );
}
