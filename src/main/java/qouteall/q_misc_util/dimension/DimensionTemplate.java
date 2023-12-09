package qouteall.q_misc_util.dimension;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.flat.FlatLayerInfo;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record DimensionTemplate(
    ResourceKey<DimensionType> dimensionTypeId,
    DimensionFactory dimensionFactory
) {
    
    public static interface DimensionFactory {
        LevelStem createLevelStem(
            MinecraftServer server,
            Holder<DimensionType> dimensionTypeHolder
        );
    }
    
    public static final Map<String, DimensionTemplate> DIMENSION_TEMPLATES = new LinkedHashMap<>();
    
    public static void registerDimensionTemplate(
        String name, DimensionTemplate dimensionTemplate
    ) {
        DIMENSION_TEMPLATES.put(name, dimensionTemplate);
    }
    
    public LevelStem createLevelStem(MinecraftServer server) {
        Registry<DimensionType> dimensionTypes =
            server.registryAccess().registryOrThrow(Registries.DIMENSION_TYPE);
        
        Holder.Reference<DimensionType> holder =
            dimensionTypes.getHolderOrThrow(dimensionTypeId);
        
        return dimensionFactory.createLevelStem(
            server, holder
        );
    }
    
    public static void init() {
        registerDimensionTemplate(
            "void", VOID_TEMPLATE
        );
    }
    
    public static final DimensionTemplate VOID_TEMPLATE = new DimensionTemplate(
        BuiltinDimensionTypes.OVERWORLD,
        (server, dimTypeHolder) -> {
            RegistryAccess.Frozen registryAccess = server.registryAccess();
            
            Registry<Biome> biomeRegistry = registryAccess.registryOrThrow(Registries.BIOME);
            
            Holder.Reference<Biome> plainsHolder = biomeRegistry.getHolderOrThrow(Biomes.PLAINS);
            
            FlatLevelGeneratorSettings flatChunkGeneratorConfig =
                new FlatLevelGeneratorSettings(
                    Optional.of(HolderSet.direct()),
                    plainsHolder,
                    List.of()
                );
            flatChunkGeneratorConfig.getLayersInfo().add(new FlatLayerInfo(1, Blocks.AIR));
            flatChunkGeneratorConfig.updateLayers();
            
            FlatLevelSource chunkGenerator = new FlatLevelSource(flatChunkGeneratorConfig);
            
            return new LevelStem(dimTypeHolder, chunkGenerator);
        }
    );
}
