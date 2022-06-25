package qouteall.q_misc_util.dimension;

import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.MiscHelper;
import qouteall.q_misc_util.api.DimensionAPI;
import qouteall.q_misc_util.mixin.dimension.IEMappedRegistry;

import java.util.HashSet;
import java.util.Set;

public class DimensionMisc {
    public static final Set<ResourceLocation> nonPersistentDimensions = new HashSet<>();
    private static final Logger logger = LogManager.getLogger();
    
    // fix the issue that nether and end get swallowed by DFU
    public static void addMissingVanillaDimensions(WorldGenSettings generatorOptions, RegistryAccess registryManager) {
        Registry<LevelStem> registry = generatorOptions.dimensions();
        long seed = generatorOptions.seed();
        if (!registry.keySet().contains(LevelStem.NETHER.location())) {
            logger.error("Missing the nether. This may be caused by DFU. Trying to fix");
            
            Registry<LevelStem> newOptions =
                DimensionType.defaultDimensions(registryManager, seed);
            
            LevelStem levelStem = newOptions.get(LevelStem.NETHER);
            
            if (levelStem != null) {
                DimensionAPI.addDimension(
                    registry,
                    LevelStem.NETHER.location(),
                    levelStem.typeHolder(),
                    levelStem.generator()
                );
            }
            else {
                Helper.err("cannot create default nether");
            }
            
            
        }
        
        if (!registry.keySet().contains(LevelStem.END.location())) {
            logger.error("Missing the end. This may be caused by DFU. Trying to fix");
            
            Registry<LevelStem> newOptions =
                DimensionType.defaultDimensions(registryManager, seed);
            
            LevelStem levelStem = newOptions.get(LevelStem.END);
            
            if (levelStem != null) {
                DimensionAPI.addDimension(
                    registry,
                    LevelStem.END.location(),
                    levelStem.typeHolder(),
                    levelStem.generator()
                );
            }
            else {
                Helper.err("cannot create default end");
            }
        }
    }
    
    public static void init() {
        DimensionAPI.serverDimensionsLoadEvent.register(DimensionMisc::addMissingVanillaDimensions);
    }
    
    // When DFU does not recognize a mod dimension (in level.dat) it will throw an error
    // then the nether and the end will be swallowed
    // to fix that, don't store the custom dimensions into level.dat
    public static MappedRegistry<LevelStem> getAdditionalDimensionsRemoved(
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
    
    public static void ensureRegistryNotFrozen(WorldGenSettings worldGenSettings) {
        Registry<LevelStem> dimensions = worldGenSettings.dimensions();
        
        ((IEMappedRegistry) dimensions).ip_setIsFrozen(false);
    }
    
    public static void ensureRegistryFrozen(WorldGenSettings worldGenSettings) {
        Registry<LevelStem> dimensions = worldGenSettings.dimensions();
        
        if (!((IEMappedRegistry) dimensions).ip_getIsFrozen()) {
            dimensions.freeze();
        }
    }
}
