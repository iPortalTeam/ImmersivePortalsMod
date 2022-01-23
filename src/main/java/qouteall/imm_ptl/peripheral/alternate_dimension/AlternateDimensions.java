package qouteall.imm_ptl.peripheral.alternate_dimension;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.data.worldgen.SurfaceRuleData;
import net.minecraft.data.worldgen.TerrainProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseSamplingSettings;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.NoiseSlider;
import net.minecraft.world.level.levelgen.StructureSettings;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.feature.configurations.StructureFeatureConfiguration;
import net.minecraft.world.level.levelgen.flat.FlatLayerInfo;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.ducks.IEWorld;
import qouteall.imm_ptl.peripheral.mixin.common.alternate_dimension.IEChunkGeneratorSettings;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.api.DimensionAPI;

import java.util.HashMap;
import java.util.Optional;

public class AlternateDimensions {
    
    public static void init() {
        DimensionAPI.serverDimensionsLoadEvent.register(AlternateDimensions::initializeAlternateDimensions);
        
        IPGlobal.postServerTickSignal.connect(AlternateDimensions::tick);
    }
    
    private static void initializeAlternateDimensions(
        WorldGenSettings generatorOptions, RegistryAccess registryManager
    ) {
        MappedRegistry<LevelStem> registry = generatorOptions.dimensions();
        long seed = generatorOptions.seed();
        if (!IPGlobal.enableAlternateDimensions) {
            return;
        }
        
        DimensionType surfaceTypeObject = registryManager.registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY).get(new ResourceLocation("immersive_portals:surface_type"));
        
        if (surfaceTypeObject == null) {
            Helper.err("Missing dimension type immersive_portals:surface_type");
            return;
        }
        
        //different seed
        DimensionAPI.addDimension(
            seed,
            registry,
            alternate1Option.location(),
            () -> surfaceTypeObject,
            createSkylandGenerator(seed + 1, registryManager)
        );
        DimensionAPI.markDimensionNonPersistent(alternate1Option.location());
        
        DimensionAPI.addDimension(
            seed,
            registry,
            alternate2Option.location(),
            () -> surfaceTypeObject,
            createSkylandGenerator(seed, registryManager)
        );
        DimensionAPI.markDimensionNonPersistent(alternate2Option.location());
        
        //different seed
        DimensionAPI.addDimension(
            seed,
            registry,
            alternate3Option.location(),
            () -> surfaceTypeObject,
            createErrorTerrainGenerator(seed + 1, registryManager)
        );
        DimensionAPI.markDimensionNonPersistent(alternate3Option.location());
        
        DimensionAPI.addDimension(
            seed,
            registry,
            alternate4Option.location(),
            () -> surfaceTypeObject,
            createErrorTerrainGenerator(seed, registryManager)
        );
        DimensionAPI.markDimensionNonPersistent(alternate4Option.location());
        
        DimensionAPI.addDimension(
            seed,
            registry,
            alternate5Option.location(),
            () -> surfaceTypeObject,
            createVoidGenerator(registryManager)
        );
        DimensionAPI.markDimensionNonPersistent(alternate5Option.location());
    }
    
    
    public static final ResourceKey<LevelStem> alternate1Option = ResourceKey.create(
        Registry.LEVEL_STEM_REGISTRY,
        new ResourceLocation("immersive_portals:alternate1")
    );
    public static final ResourceKey<LevelStem> alternate2Option = ResourceKey.create(
        Registry.LEVEL_STEM_REGISTRY,
        new ResourceLocation("immersive_portals:alternate2")
    );
    public static final ResourceKey<LevelStem> alternate3Option = ResourceKey.create(
        Registry.LEVEL_STEM_REGISTRY,
        new ResourceLocation("immersive_portals:alternate3")
    );
    public static final ResourceKey<LevelStem> alternate4Option = ResourceKey.create(
        Registry.LEVEL_STEM_REGISTRY,
        new ResourceLocation("immersive_portals:alternate4")
    );
    public static final ResourceKey<LevelStem> alternate5Option = ResourceKey.create(
        Registry.LEVEL_STEM_REGISTRY,
        new ResourceLocation("immersive_portals:alternate5")
    );
    public static final ResourceKey<DimensionType> surfaceType = ResourceKey.create(
        Registry.DIMENSION_TYPE_REGISTRY,
        new ResourceLocation("immersive_portals:surface_type")
    );
    public static final ResourceKey<Level> alternate1 = ResourceKey.create(
        Registry.DIMENSION_REGISTRY,
        new ResourceLocation("immersive_portals:alternate1")
    );
    public static final ResourceKey<Level> alternate2 = ResourceKey.create(
        Registry.DIMENSION_REGISTRY,
        new ResourceLocation("immersive_portals:alternate2")
    );
    public static final ResourceKey<Level> alternate3 = ResourceKey.create(
        Registry.DIMENSION_REGISTRY,
        new ResourceLocation("immersive_portals:alternate3")
    );
    public static final ResourceKey<Level> alternate4 = ResourceKey.create(
        Registry.DIMENSION_REGISTRY,
        new ResourceLocation("immersive_portals:alternate4")
    );
    public static final ResourceKey<Level> alternate5 = ResourceKey.create(
        Registry.DIMENSION_REGISTRY,
        new ResourceLocation("immersive_portals:alternate5")
    );
//    public static DimensionType surfaceTypeObject;
    
    public static boolean isAlternateDimension(Level world) {
        final ResourceKey<Level> key = world.dimension();
        return key == alternate1 ||
            key == alternate2 ||
            key == alternate3 ||
            key == alternate4 ||
            key == alternate5;
    }
    
    private static void syncWithOverworldTimeWeather(ServerLevel world, ServerLevel overworld) {
        ((IEWorld) world).portal_setWeather(
            overworld.getRainLevel(1), overworld.getRainLevel(1),
            overworld.getThunderLevel(1), overworld.getThunderLevel(1)
        );
    }
    
    public static ChunkGenerator createSkylandGenerator(long seed, RegistryAccess rm) {
        
        Registry<Biome> biomeRegistry = rm.registryOrThrow(Registry.BIOME_REGISTRY);
        MultiNoiseBiomeSource biomeSource = MultiNoiseBiomeSource.Preset.OVERWORLD.biomeSource(
            biomeRegistry, true
        );
        
        Registry<NoiseGeneratorSettings> settingsRegistry = rm.registryOrThrow(Registry.NOISE_GENERATOR_SETTINGS_REGISTRY);
        
        HashMap<StructureFeature<?>, StructureFeatureConfiguration> structureMap = new HashMap<>();
        structureMap.putAll(StructureSettings.DEFAULTS);
        structureMap.remove(StructureFeature.MINESHAFT);
        structureMap.remove(StructureFeature.STRONGHOLD);
        
        StructureSettings structuresConfig = new StructureSettings(
            Optional.empty(), structureMap
        );
        
        NoiseGeneratorSettings skylandSetting = createIslandSettings(
            structuresConfig, Blocks.STONE.defaultBlockState(),
            Blocks.WATER.defaultBlockState()
        );
        
        NoiseBasedChunkGenerator islandChunkGenerator = new NoiseBasedChunkGenerator(
            rm.registryOrThrow(Registry.NOISE_REGISTRY),
            biomeSource, seed, () -> skylandSetting
        );
        
        NoiseGeneratorSettings surfaceSetting = createSurfaceSettings(
            structuresConfig, Blocks.STONE.defaultBlockState(),
            Blocks.WATER.defaultBlockState()
        );
        
        NoiseBasedChunkGenerator surfaceChunkGenerator = new NoiseBasedChunkGenerator(
            rm.registryOrThrow(Registry.NOISE_REGISTRY),
            biomeSource, seed, () -> surfaceSetting
        );
        
        return new DelegatedChunkGenerator.SpecialNoise(
            biomeSource, structuresConfig,
            surfaceChunkGenerator, islandChunkGenerator
        );
    }
    
    public static ChunkGenerator createErrorTerrainGenerator(long seed, RegistryAccess rm) {
        Registry<Biome> biomeRegistry = rm.registryOrThrow(Registry.BIOME_REGISTRY);
        
        ChaosBiomeSource chaosBiomeSource = new ChaosBiomeSource(seed, biomeRegistry);
        return new ErrorTerrainGenerator(seed, createSkylandGenerator(seed, rm), chaosBiomeSource);
    }
    
    public static ChunkGenerator createVoidGenerator(RegistryAccess rm) {
        Registry<Biome> biomeRegistry = rm.registryOrThrow(Registry.BIOME_REGISTRY);
        
        StructureSettings structuresConfig = new StructureSettings(
            Optional.of(StructureSettings.DEFAULT_STRONGHOLD),
            Maps.newHashMap(ImmutableMap.of())
        );
        FlatLevelGeneratorSettings flatChunkGeneratorConfig =
            new FlatLevelGeneratorSettings(structuresConfig, biomeRegistry);
        flatChunkGeneratorConfig.getLayersInfo().add(new FlatLayerInfo(1, Blocks.AIR));
        flatChunkGeneratorConfig.updateLayers();
        
        return new FlatLevelSource(flatChunkGeneratorConfig);
    }
    
    
    private static void tick() {
        if (!IPGlobal.enableAlternateDimensions) {
            return;
        }
        
        ServerLevel overworld = McHelper.getServerWorld(Level.OVERWORLD);
        
        syncWithOverworldTimeWeather(McHelper.getServerWorld(alternate1), overworld);
        syncWithOverworldTimeWeather(McHelper.getServerWorld(alternate2), overworld);
        syncWithOverworldTimeWeather(McHelper.getServerWorld(alternate3), overworld);
        syncWithOverworldTimeWeather(McHelper.getServerWorld(alternate4), overworld);
        syncWithOverworldTimeWeather(McHelper.getServerWorld(alternate5), overworld);
    }
    
    /**
     * vanilla copy
     * {@link ChunkGeneratorSettings}
     */
    private static NoiseGeneratorSettings createIslandSettings(
        StructureSettings structuresConfig, BlockState defaultBlock, BlockState defaultFluid
    ) {
        return IEChunkGeneratorSettings.construct(
            structuresConfig,
            NoiseSettings.create(
                0, 128,
                new NoiseSamplingSettings(
                    2.0, 1.0, 80.0, 160.0
                ),
                new NoiseSlider(-23.4375, 64, -46),
                new NoiseSlider(-0.234375, 7, 1),
                2, 1, false, false, false,
//                VanillaTerrainParametersCreator.createNetherParameters()
//                VanillaTerrainParametersCreator.createSurfaceParameters(false)
                TerrainProvider.floatingIslands()
            ),
            defaultBlock, defaultFluid,
            SurfaceRuleData.overworldLike(true, false, false),
            0, false, false, false, false, false,
            false
        );
        
    }
    
    private static NoiseGeneratorSettings createSurfaceSettings(
        StructureSettings structuresConfig, BlockState defaultBlock, BlockState defaultFluid
    ) {
        return IEChunkGeneratorSettings.construct(
            structuresConfig,
            NoiseSettings.create(
                0, 128,
                new NoiseSamplingSettings(
                    2.0, 1.0, 80.0, 160.0
                ),
                new NoiseSlider(-23.4375, 64, -46),
                new NoiseSlider(-0.234375, 7, 1),
                2, 1, false, false, false,
//                VanillaTerrainParametersCreator.createNetherParameters()
                TerrainProvider.overworld(false)
//                VanillaTerrainParametersCreator.createFloatingIslandsParameters()
            ),
            defaultBlock, defaultFluid,
            SurfaceRuleData.overworldLike(true, false, false),
            0, false, false, false, false, false,
            false
        );
        
    }
}
