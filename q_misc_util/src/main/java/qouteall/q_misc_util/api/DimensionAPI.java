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
import qouteall.q_misc_util.MiscHelper;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

public class DimensionAPI {
    private static final Logger logger = LogManager.getLogger();
    
    public static interface ServerDimensionsLoadCallback {
        void run(WorldGenSettings generatorOptions, RegistryAccess registryManager);
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
    
    private static final Set<ResourceLocation> nonPersistentDimensions = new HashSet<>();
    
    public static void addDimension(
        long argSeed,
        Registry<LevelStem> dimensionOptionsRegistry,
        ResourceLocation dimensionId,
        Holder<DimensionType> dimensionTypeHolder,
        ChunkGenerator chunkGenerator
    ) {
        if (dimensionOptionsRegistry instanceof MappedRegistry mapped) {
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
    
    public static void markDimensionNonPersistent(ResourceLocation dimensionId) {
        nonPersistentDimensions.add(dimensionId);
    }
    
    // This is not API
    // When DFU does not recognize a mod dimension (in level.dat) it will throw an error
    // then the nether and the end will be swallowed (https://github.com/TelepathicGrunt/Bumblezone-Fabric/issues/20)
    // to fix that, don't store the custom dimensions into level.dat
    public static MappedRegistry<LevelStem> _getAdditionalDimensionsRemoved(
        MappedRegistry<LevelStem> registry
    ) {
        if (nonPersistentDimensions.isEmpty()) {
            return registry;
        }
        
        return MiscHelper.filterAndCopyRegistry(
            registry,
            (key, obj) -> {
                ResourceLocation identifier = key.location();
                return !nonPersistentDimensions.contains(identifier);
            }
        );
    }
    
}
