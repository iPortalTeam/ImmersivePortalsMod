package qouteall.q_misc_util.dimension;

import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.MiscHelper;
import qouteall.q_misc_util.api.DimensionAPI;
import qouteall.q_misc_util.mixin.dimension.IEMappedRegistry;

import java.util.HashSet;
import java.util.Set;

public class DimensionMisc {
    private static final Logger logger = LogManager.getLogger();
    
    // TODO know whether it's still useful in 1.19.3
    @Deprecated
    public static void addMissingVanillaDimensions(WorldGenSettings generatorOptions, RegistryAccess registryManager) {
        // probably no longer needed
        
//        Registry<LevelStem> registry = generatorOptions.dimensions();
//        long seed = generatorOptions.seed();
//        if (!registry.keySet().contains(LevelStem.NETHER.location())) {
//            logger.error("Missing the nether. This may be caused by DFU. Trying to fix");
//
//            WorldPreset worldPreset = BuiltinRegistries.WORLD_PRESET.stream().findFirst().orElseThrow();
//
//            WorldGenSettings worldGenSettings = worldPreset.recreateWorldGenSettings(generatorOptions);
//
//            LevelStem levelStem = worldGenSettings.dimensions().get(LevelStem.NETHER);
//
//            if (levelStem != null) {
//                DimensionAPI.addDimension(
//                    registry,
//                    LevelStem.NETHER.location(),
//                    levelStem.typeHolder(),
//                    levelStem.generator()
//                );
//            }
//            else {
//                Helper.err("cannot create default nether");
//            }
//        }
//
//        if (!registry.keySet().contains(LevelStem.END.location())) {
//            logger.error("Missing the end. This may be caused by DFU. Trying to fix");
//
//            WorldPreset worldPreset = BuiltinRegistries.WORLD_PRESET.stream().findFirst().orElseThrow();
//
//            WorldGenSettings worldGenSettings = worldPreset.recreateWorldGenSettings(generatorOptions);
//
//            LevelStem levelStem = worldGenSettings.dimensions().get(LevelStem.END);
//
//            if (levelStem != null) {
//                DimensionAPI.addDimension(
//                    registry,
//                    LevelStem.END.location(),
//                    levelStem.typeHolder(),
//                    levelStem.generator()
//                );
//            }
//            else {
//                Helper.err("cannot create default end");
//            }
//        }
    }
    
    public static void init() {
//        DimensionAPI.serverDimensionsLoadEvent.register(DimensionMisc::addMissingVanillaDimensions);
    }
    
}
