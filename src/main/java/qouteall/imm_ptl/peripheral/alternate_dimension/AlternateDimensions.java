package qouteall.imm_ptl.peripheral.alternate_dimension;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import net.minecraft.block.BlockState;
import net.minecraft.world.biome.source.MultiNoiseBiomeSource;
import net.minecraft.world.biome.source.util.VanillaTerrainParametersCreator;
import net.minecraft.world.gen.chunk.GenerationShapeConfig;
import net.minecraft.world.gen.chunk.NoiseSamplingConfig;
import net.minecraft.world.gen.chunk.SlideConfig;
import net.minecraft.world.gen.surfacebuilder.VanillaSurfaceRules;
import qouteall.imm_ptl.peripheral.mixin.common.alternate_dimension.IEChunkGeneratorSettings;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.q_misc_util.Helper;
import qouteall.imm_ptl.core.McHelper;
import qouteall.q_misc_util.api.DimensionAPI;
import qouteall.imm_ptl.core.ducks.IEWorld;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.util.registry.SimpleRegistry;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.chunk.FlatChunkGenerator;
import net.minecraft.world.gen.chunk.FlatChunkGeneratorConfig;
import net.minecraft.world.gen.chunk.FlatChunkGeneratorLayer;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
import net.minecraft.world.gen.chunk.StructureConfig;
import net.minecraft.world.gen.chunk.StructuresConfig;
import net.minecraft.world.gen.feature.StructureFeature;

import java.util.HashMap;
import java.util.Optional;

public class AlternateDimensions {
    // temporary workaround
    public static boolean isCreatingSkylandGenerator = false;
    
    public static void init() {
        DimensionAPI.serverDimensionsLoadEvent.register(AlternateDimensions::initializeAlternateDimensions);
        
        IPGlobal.postServerTickSignal.connect(AlternateDimensions::tick);
    }
    
    private static void initializeAlternateDimensions(
        GeneratorOptions generatorOptions, DynamicRegistryManager registryManager
    ) {
        SimpleRegistry<DimensionOptions> registry = generatorOptions.getDimensions();
        long seed = generatorOptions.getSeed();
        if (!IPGlobal.enableAlternateDimensions) {
            return;
        }
        
        DimensionType surfaceTypeObject = registryManager.get(Registry.DIMENSION_TYPE_KEY).get(new Identifier("immersive_portals:surface_type"));
        
        if (surfaceTypeObject == null) {
            Helper.err("Missing dimension type immersive_portals:surface_type");
            return;
        }
        
        //different seed
        DimensionAPI.addDimension(
            seed,
            registry,
            alternate1Option.getValue(),
            () -> surfaceTypeObject,
            createSkylandGenerator(seed + 1, registryManager)
        );
        DimensionAPI.markDimensionNonPersistent(alternate1Option.getValue());
        
        DimensionAPI.addDimension(
            seed,
            registry,
            alternate2Option.getValue(),
            () -> surfaceTypeObject,
            createSkylandGenerator(seed, registryManager)
        );
        DimensionAPI.markDimensionNonPersistent(alternate2Option.getValue());
        
        //different seed
        DimensionAPI.addDimension(
            seed,
            registry,
            alternate3Option.getValue(),
            () -> surfaceTypeObject,
            createErrorTerrainGenerator(seed + 1, registryManager)
        );
        DimensionAPI.markDimensionNonPersistent(alternate3Option.getValue());
        
        DimensionAPI.addDimension(
            seed,
            registry,
            alternate4Option.getValue(),
            () -> surfaceTypeObject,
            createErrorTerrainGenerator(seed, registryManager)
        );
        DimensionAPI.markDimensionNonPersistent(alternate4Option.getValue());
        
        DimensionAPI.addDimension(
            seed,
            registry,
            alternate5Option.getValue(),
            () -> surfaceTypeObject,
            createVoidGenerator(registryManager)
        );
        DimensionAPI.markDimensionNonPersistent(alternate5Option.getValue());
    }
    
    
    public static final RegistryKey<DimensionOptions> alternate1Option = RegistryKey.of(
        Registry.DIMENSION_KEY,
        new Identifier("immersive_portals:alternate1")
    );
    public static final RegistryKey<DimensionOptions> alternate2Option = RegistryKey.of(
        Registry.DIMENSION_KEY,
        new Identifier("immersive_portals:alternate2")
    );
    public static final RegistryKey<DimensionOptions> alternate3Option = RegistryKey.of(
        Registry.DIMENSION_KEY,
        new Identifier("immersive_portals:alternate3")
    );
    public static final RegistryKey<DimensionOptions> alternate4Option = RegistryKey.of(
        Registry.DIMENSION_KEY,
        new Identifier("immersive_portals:alternate4")
    );
    public static final RegistryKey<DimensionOptions> alternate5Option = RegistryKey.of(
        Registry.DIMENSION_KEY,
        new Identifier("immersive_portals:alternate5")
    );
    public static final RegistryKey<DimensionType> surfaceType = RegistryKey.of(
        Registry.DIMENSION_TYPE_KEY,
        new Identifier("immersive_portals:surface_type")
    );
    public static final RegistryKey<World> alternate1 = RegistryKey.of(
        Registry.WORLD_KEY,
        new Identifier("immersive_portals:alternate1")
    );
    public static final RegistryKey<World> alternate2 = RegistryKey.of(
        Registry.WORLD_KEY,
        new Identifier("immersive_portals:alternate2")
    );
    public static final RegistryKey<World> alternate3 = RegistryKey.of(
        Registry.WORLD_KEY,
        new Identifier("immersive_portals:alternate3")
    );
    public static final RegistryKey<World> alternate4 = RegistryKey.of(
        Registry.WORLD_KEY,
        new Identifier("immersive_portals:alternate4")
    );
    public static final RegistryKey<World> alternate5 = RegistryKey.of(
        Registry.WORLD_KEY,
        new Identifier("immersive_portals:alternate5")
    );
//    public static DimensionType surfaceTypeObject;
    
    public static boolean isAlternateDimension(World world) {
        final RegistryKey<World> key = world.getRegistryKey();
        return key == alternate1 ||
            key == alternate2 ||
            key == alternate3 ||
            key == alternate4 ||
            key == alternate5;
    }
    
    private static void syncWithOverworldTimeWeather(ServerWorld world, ServerWorld overworld) {
        ((IEWorld) world).portal_setWeather(
            overworld.getRainGradient(1), overworld.getRainGradient(1),
            overworld.getThunderGradient(1), overworld.getThunderGradient(1)
        );
    }
    
    public static ChunkGenerator createSkylandGenerator(long seed, DynamicRegistryManager rm) {
        
        Registry<Biome> biomeRegistry = rm.get(Registry.BIOME_KEY);
        MultiNoiseBiomeSource biomeSource = MultiNoiseBiomeSource.Preset.OVERWORLD.getBiomeSource(
            biomeRegistry, true
        );
        
        Registry<ChunkGeneratorSettings> settingsRegistry = rm.get(Registry.CHUNK_GENERATOR_SETTINGS_KEY);
        
        HashMap<StructureFeature<?>, StructureConfig> structureMap = new HashMap<>();
        structureMap.putAll(StructuresConfig.DEFAULT_STRUCTURES);
        structureMap.remove(StructureFeature.MINESHAFT);
        structureMap.remove(StructureFeature.STRONGHOLD);
        
        StructuresConfig structuresConfig = new StructuresConfig(
            Optional.empty(), structureMap
        );
        
        ChunkGeneratorSettings skylandSetting = createIslandSettings(
            structuresConfig, Blocks.STONE.getDefaultState(),
            Blocks.WATER.getDefaultState(), false, false
        );
        
        return new NoiseChunkGenerator(
            rm.get(Registry.NOISE_WORLDGEN),
            biomeSource, seed, () -> skylandSetting
        );
    }
    
    public static ChunkGenerator createErrorTerrainGenerator(long seed, DynamicRegistryManager rm) {
        Registry<Biome> biomeRegistry = rm.get(Registry.BIOME_KEY);
        
        ChaosBiomeSource chaosBiomeSource = new ChaosBiomeSource(seed, biomeRegistry);
        return new ErrorTerrainGenerator(seed, createSkylandGenerator(seed, rm), chaosBiomeSource);
    }
    
    public static ChunkGenerator createVoidGenerator(DynamicRegistryManager rm) {
        Registry<Biome> biomeRegistry = rm.get(Registry.BIOME_KEY);
        
        StructuresConfig structuresConfig = new StructuresConfig(
            Optional.of(StructuresConfig.DEFAULT_STRONGHOLD),
            Maps.newHashMap(ImmutableMap.of())
        );
        FlatChunkGeneratorConfig flatChunkGeneratorConfig =
            new FlatChunkGeneratorConfig(structuresConfig, biomeRegistry);
        flatChunkGeneratorConfig.getLayers().add(new FlatChunkGeneratorLayer(1, Blocks.AIR));
        flatChunkGeneratorConfig.updateLayerBlocks();
        
        return new FlatChunkGenerator(flatChunkGeneratorConfig);
    }
    
    
    private static void tick() {
        if (!IPGlobal.enableAlternateDimensions) {
            return;
        }
        
        ServerWorld overworld = McHelper.getServerWorld(World.OVERWORLD);
        
        syncWithOverworldTimeWeather(McHelper.getServerWorld(alternate1), overworld);
        syncWithOverworldTimeWeather(McHelper.getServerWorld(alternate2), overworld);
        syncWithOverworldTimeWeather(McHelper.getServerWorld(alternate3), overworld);
        syncWithOverworldTimeWeather(McHelper.getServerWorld(alternate4), overworld);
        syncWithOverworldTimeWeather(McHelper.getServerWorld(alternate5), overworld);
    }
    
    // vanilla copy
    private static ChunkGeneratorSettings createIslandSettings(
        StructuresConfig structuresConfig, BlockState defaultBlock, BlockState defaultFluid,
        boolean bl, boolean bl2
    ) {
        return IEChunkGeneratorSettings.construct(
            structuresConfig,
            GenerationShapeConfig.create(
                0, 128,
                new NoiseSamplingConfig(
                    2.0, 1.0, 80.0, 160.0
                ),
                new SlideConfig(-23.4375, 64, -46),
                new SlideConfig(-0.234375, 7, 1),
                2, 1, bl2, false, false,
                VanillaTerrainParametersCreator.createIslandParameters()
            ),
            defaultBlock, defaultFluid, VanillaSurfaceRules.createOverworldSurfaceRule(),
            0, bl, false, false, false, false,
            true
        );
        
    }
}
