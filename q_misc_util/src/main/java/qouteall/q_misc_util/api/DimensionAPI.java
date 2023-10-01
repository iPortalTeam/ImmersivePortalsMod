package qouteall.q_misc_util.api;

import com.mojang.serialization.Lifecycle;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.core.Holder;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
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
import qouteall.q_misc_util.mixin.dimension.IELayeredRegistryAccess;
import qouteall.q_misc_util.mixin.dimension.IEMappedRegistry;

import java.util.List;
import java.util.Set;

public class DimensionAPI {
    private static final Logger LOGGER = LogManager.getLogger();
    
    public static interface ServerDimensionsLoadCallback {
        /**
         * TODO update doc
         */
        void run(WorldOptions worldOptions, RegistryAccess registryAccess);
    }
    
    /**
     * This event is fired when loading custom dimensions when the server is starting.
     * Note: when showing the dimension list on client dimension stack menu, this event will also be fired.
     */
    public static final Event<ServerDimensionsLoadCallback> serverDimensionsLoadEvent =
        EventFactory.createArrayBacked(
            ServerDimensionsLoadCallback.class,
            (listeners) -> ((worldOptions, registryManager) -> {
                Registry<LevelStem> levelStems = registryManager.registryOrThrow(Registries.LEVEL_STEM);
                for (ServerDimensionsLoadCallback listener : listeners) {
                    try {
                        listener.run(worldOptions, registryManager);
                    }
                    catch (Exception e) {
                        LOGGER.error("Error registering custom dimension", e);
                    }
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
     * Add a new dimension when the server is running.
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
     * The removed dimension's config will be removed from the `level.dat` file.
     * Cannot be used during server initialization. Cannot remove vanilla dimensions.
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
     * Will be triggered when the server dynamically add or remove a dimension
     * Does not get triggered during server initialization
     */
    public static final Event<ServerDynamicUpdateListener> serverDimensionDynamicUpdateEvent =
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
    public static final Event<ClientDynamicUpdateListener> clientDimensionUpdateEvent =
        EventFactory.createArrayBacked(
            ClientDynamicUpdateListener.class,
            arr -> (set) -> {
                for (ClientDynamicUpdateListener runnable : arr) {
                    runnable.run(set);
                }
            }
        );
    
    /**
     * This is called when opening "Add Dimension" GUI in dimension stack
     */
    @Deprecated
    public static MappedRegistry<LevelStem> collectCustomDimensions(
        RegistryAccess.Frozen worldGenLoadContext,
        WorldOptions options
    ) {
        MappedRegistry<LevelStem> subDimensionRegistry = new MappedRegistry<>(Registries.LEVEL_STEM, Lifecycle.stable());
        
        RegistryAccess.Frozen subRegistryAccess =
            new RegistryAccess.ImmutableRegistryAccess(List.of(subDimensionRegistry)).freeze();
        
        LayeredRegistryAccess<Integer> wrappedLayeredRegistryAccess = IELayeredRegistryAccess.ip_init(
            List.of(1, 2),
            List.of(worldGenLoadContext, subRegistryAccess)
        );
        RegistryAccess.Frozen wrappedRegistryAccess = wrappedLayeredRegistryAccess.compositeAccess();
        
        DimensionAPI.serverDimensionsLoadEvent.invoker().run(options, wrappedRegistryAccess);
        
        return subDimensionRegistry;
    }
}
