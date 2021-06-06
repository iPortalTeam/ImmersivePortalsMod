package qouteall.q_misc_util.api;

import com.mojang.serialization.Lifecycle;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
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
import qouteall.q_misc_util.DimensionMisc;
import qouteall.q_misc_util.MiscHelper;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

public class DimensionAPI {
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
    public static SimpleRegistry<DimensionOptions> _getAdditionalDimensionsRemoved(
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
    
}
