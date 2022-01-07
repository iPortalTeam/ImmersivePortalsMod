package qouteall.q_misc_util;

import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.util.registry.SimpleRegistry;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.GeneratorOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qouteall.q_misc_util.api.DimensionAPI;
import qouteall.q_misc_util.mixin.dimension.DimensionTypeAccessor;

public class DimensionMisc {
    private static final Logger logger = LogManager.getLogger();
    
    // fix the issue that nether and end get swallowed by DFU
    public static void addMissingVanillaDimensions(GeneratorOptions generatorOptions, DynamicRegistryManager registryManager) {
        SimpleRegistry<DimensionOptions> registry = generatorOptions.getDimensions();
        long seed = generatorOptions.getSeed();
        if (!registry.getIds().contains(DimensionOptions.NETHER.getValue())) {
            logger.error("Missing the nether. This may be caused by DFU. Trying to fix");
            
            SimpleRegistry<DimensionOptions> newOptions =
                DimensionType.createDefaultDimensionOptions(registryManager, seed);
            
            DimensionAPI.addDimension(
                seed,
                registry,
                DimensionOptions.NETHER.getValue(),
                () -> DimensionTypeAccessor._getTheNether(),
                newOptions.get(DimensionOptions.NETHER).getChunkGenerator()
            );
        }
        
        if (!registry.getIds().contains(DimensionOptions.END.getValue())) {
            logger.error("Missing the end. This may be caused by DFU. Trying to fix");
            
            SimpleRegistry<DimensionOptions> newOptions =
                DimensionType.createDefaultDimensionOptions(registryManager, seed);
            
            DimensionAPI.addDimension(
                seed,
                registry,
                DimensionOptions.END.getValue(),
                () -> DimensionTypeAccessor._getTheEnd(),
                newOptions.get(DimensionOptions.END).getChunkGenerator()
            );
        }
    }
    
    public static void init() {
        DimensionAPI.serverDimensionsLoadEvent.register(DimensionMisc::addMissingVanillaDimensions);
    }
}
