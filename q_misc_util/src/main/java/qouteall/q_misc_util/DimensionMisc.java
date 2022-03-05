package qouteall.q_misc_util;

import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qouteall.q_misc_util.api.DimensionAPI;
import qouteall.q_misc_util.mixin.dimension.DimensionTypeAccessor;

public class DimensionMisc {
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
                    seed,
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
                    seed,
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
}
