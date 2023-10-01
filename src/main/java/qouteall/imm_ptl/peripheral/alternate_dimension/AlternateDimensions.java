package qouteall.imm_ptl.peripheral.alternate_dimension;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.flat.FlatLayerInfo;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import org.jetbrains.annotations.Nullable;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.ducks.IEWorld;
import qouteall.q_misc_util.api.DimensionAPI;

import java.util.List;
import java.util.Optional;

public class AlternateDimensions {
    
    public static final ResourceKey<DimensionType> SURFACE_TYPE = ResourceKey.create(
        Registries.DIMENSION_TYPE,
        new ResourceLocation("immersive_portals:surface_type")
    );
    
    public static final ResourceKey<DimensionType> SURFACE_TYPE_BRIGHT = ResourceKey.create(
        Registries.DIMENSION_TYPE,
        new ResourceLocation("immersive_portals:surface_type_bright")
    );
    
    public static void init() {
        DimensionAPI.SEVER_DIMENSIONS_LOAD_EVENT.register(AlternateDimensions::initializeAlternateDimensions);
        
        ServerTickEvents.END_SERVER_TICK.register(AlternateDimensions::tick);
    }
    
    private static void initializeAlternateDimensions(
        MinecraftServer server,
        WorldOptions worldOptions,
        MappedRegistry<LevelStem> levelStemRegistry,
        RegistryAccess registryManager
    ) {
        long seed = worldOptions.seed();
        if (!IPGlobal.enableAlternateDimensions) {
            return;
        }
        
        Holder<DimensionType> surfaceTypeHolder = registryManager
            .registryOrThrow(Registries.DIMENSION_TYPE)
            .getHolder(SURFACE_TYPE)
            .orElseThrow(() -> new RuntimeException("Missing immersive_portals:surface_type"));
        
        Holder<DimensionType> surfaceTypeBrightHolder = registryManager
            .registryOrThrow(Registries.DIMENSION_TYPE)
            .getHolder(SURFACE_TYPE_BRIGHT)
            .orElseThrow(() -> new RuntimeException("Missing immersive_portals:surface_type_bright"));
        
        DimensionAPI.addDimensionToRegistry(
            levelStemRegistry,
            alternate1.location(),
            surfaceTypeBrightHolder,
            createSkylandGenerator(registryManager, seed)
        );
        
        DimensionAPI.addDimensionToRegistry(
            levelStemRegistry,
            alternate2.location(),
            surfaceTypeHolder,
            createSkylandGenerator(registryManager, seed + 1) // different seed
        );
        
        DimensionAPI.addDimensionToRegistry(
            levelStemRegistry,
            alternate3.location(),
            surfaceTypeHolder,
            createErrorTerrainGenerator(seed + 1, registryManager)
        );
        
        DimensionAPI.addDimensionToRegistry(
            levelStemRegistry,
            alternate4.location(),
            surfaceTypeHolder,
            createErrorTerrainGenerator(seed, registryManager)
        );
        
        DimensionAPI.addDimensionToRegistry(
            levelStemRegistry,
            alternate5.location(),
            surfaceTypeHolder,
            createVoidGenerator(registryManager)
        );
    }
    
    public static final ResourceKey<Level> alternate1 = ResourceKey.create(
        Registries.DIMENSION,
        new ResourceLocation("immersive_portals:alternate1")
    );
    public static final ResourceKey<Level> alternate2 = ResourceKey.create(
        Registries.DIMENSION,
        new ResourceLocation("immersive_portals:alternate2")
    );
    public static final ResourceKey<Level> alternate3 = ResourceKey.create(
        Registries.DIMENSION,
        new ResourceLocation("immersive_portals:alternate3")
    );
    public static final ResourceKey<Level> alternate4 = ResourceKey.create(
        Registries.DIMENSION,
        new ResourceLocation("immersive_portals:alternate4")
    );
    public static final ResourceKey<Level> alternate5 = ResourceKey.create(
        Registries.DIMENSION,
        new ResourceLocation("immersive_portals:alternate5")
    );
    
    public static boolean isAlternateDimension(Level world) {
        final ResourceKey<Level> key = world.dimension();
        return key == alternate1 ||
            key == alternate2 ||
            key == alternate3 ||
            key == alternate4 ||
            key == alternate5;
    }
    
    private static void syncWithOverworldTimeWeather(@Nullable ServerLevel world, ServerLevel overworld) {
        if (world == null) {
            return;
        }
        ((IEWorld) world).portal_setWeather(
            overworld.getRainLevel(1), overworld.getRainLevel(1),
            overworld.getThunderLevel(1), overworld.getThunderLevel(1)
        );
    }
    
    public static ChunkGenerator createSkylandGenerator(RegistryAccess rm, long seed) {
        return NormalSkylandGenerator.create(
            rm.registryOrThrow(Registries.BIOME).asLookup(),
            rm.registryOrThrow(Registries.DENSITY_FUNCTION).asLookup(),
            rm.registryOrThrow(Registries.NOISE).asLookup(),
            rm.registryOrThrow(Registries.NOISE_SETTINGS).asLookup(),
            rm.registryOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST).asLookup(),
            seed
        );
    }
    
    public static ChunkGenerator createErrorTerrainGenerator(long seed, RegistryAccess rm) {
        return ErrorTerrainGenerator.create(
            rm.registryOrThrow(Registries.BIOME).asLookup(),
            rm.registryOrThrow(Registries.NOISE_SETTINGS).asLookup()
        );
    }
    
    public static ChunkGenerator createVoidGenerator(RegistryAccess rm) {
        Registry<Biome> biomeRegistry = rm.registryOrThrow(Registries.BIOME);
        
        Registry<StructureSet> structureSets = rm.registryOrThrow(Registries.STRUCTURE_SET);
        
        Holder.Reference<Biome> plainsHolder = biomeRegistry.getHolderOrThrow(Biomes.PLAINS);
        
        FlatLevelGeneratorSettings flatChunkGeneratorConfig =
            new FlatLevelGeneratorSettings(
                Optional.of(HolderSet.direct()),
                plainsHolder,
                List.of()
            );
        flatChunkGeneratorConfig.getLayersInfo().add(new FlatLayerInfo(1, Blocks.AIR));
        flatChunkGeneratorConfig.updateLayers();
        
        return new FlatLevelSource(flatChunkGeneratorConfig);
    }
    
    
    private static void tick(MinecraftServer server) {
        // TODO check whether manual weather sync necessary
        // or sync weather based on dimension type
//        if (!IPGlobal.enableAlternateDimensions) {
//            return;
//        }
//
//        ServerLevel overworld = McHelper.getServerWorld(Level.OVERWORLD);
//
//        MinecraftServer server = MiscHelper.getServer();
//
//        syncWithOverworldTimeWeather(server.getLevel(alternate1), overworld);
//        syncWithOverworldTimeWeather(server.getLevel(alternate2), overworld);
//        syncWithOverworldTimeWeather(server.getLevel(alternate3), overworld);
//        syncWithOverworldTimeWeather(server.getLevel(alternate4), overworld);
//        syncWithOverworldTimeWeather(server.getLevel(alternate5), overworld);
    }
}
