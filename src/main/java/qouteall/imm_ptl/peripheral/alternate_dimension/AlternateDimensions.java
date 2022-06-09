package qouteall.imm_ptl.peripheral.alternate_dimension;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.data.worldgen.SurfaceRuleData;
import net.minecraft.data.worldgen.TerrainProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
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
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.flat.FlatLayerInfo;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.ducks.IEWorld;
import qouteall.imm_ptl.peripheral.mixin.common.alternate_dimension.IENoiseGeneratorSettings;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.MiscHelper;
import qouteall.q_misc_util.api.DimensionAPI;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AlternateDimensions {
    
    public static void init() {
        DimensionAPI.serverDimensionsLoadEvent.register(AlternateDimensions::initializeAlternateDimensions);
        
        IPGlobal.postServerTickSignal.connect(AlternateDimensions::tick);
    }
    
    private static void initializeAlternateDimensions(
        WorldGenSettings generatorOptions, RegistryAccess registryManager
    ) {
        Registry<LevelStem> registry = generatorOptions.dimensions();
        long seed = generatorOptions.seed();
        if (!IPGlobal.enableAlternateDimensions) {
            return;
        }
        
        ResourceKey<DimensionType> resourceKey = ResourceKey.create(
            Registry.DIMENSION_TYPE_REGISTRY,
            new ResourceLocation("immersive_portals:surface_type")
        );
        Holder<DimensionType> surfaceTypeHolder = registryManager
            .registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY)
            .getHolder(resourceKey)
            .orElseThrow(() -> new RuntimeException("Missing immersive_portals:surface_type"));
        
        if (surfaceTypeHolder == null) {
            Helper.err("Missing dimension type immersive_portals:surface_type");
            return;
        }
        
        //different seed
        DimensionAPI.addDimension(
            registry,
            alternate1.location(),
            surfaceTypeHolder,
            createSkylandGenerator(registryManager)
        );
        DimensionAPI.markDimensionNonPersistent(alternate1.location());
        
        DimensionAPI.addDimension(
            registry,
            alternate2.location(),
            surfaceTypeHolder,
            createSkylandGenerator(registryManager)
        );
        DimensionAPI.markDimensionNonPersistent(alternate2.location());
        
        //different seed
        DimensionAPI.addDimension(
            registry,
            alternate3.location(),
            surfaceTypeHolder,
            createErrorTerrainGenerator(seed + 1, registryManager)
        );
        DimensionAPI.markDimensionNonPersistent(alternate3.location());
        
        DimensionAPI.addDimension(
            registry,
            alternate4.location(),
            surfaceTypeHolder,
            createErrorTerrainGenerator(seed, registryManager)
        );
        DimensionAPI.markDimensionNonPersistent(alternate4.location());
        
        DimensionAPI.addDimension(
            registry,
            alternate5.location(),
            surfaceTypeHolder,
            createVoidGenerator(registryManager)
        );
        DimensionAPI.markDimensionNonPersistent(alternate5.location());
    }
    
    
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
    
    public static ChunkGenerator createSkylandGenerator(RegistryAccess rm) {
        return NormalSkylandGenerator.create(
            rm.registryOrThrow(Registry.STRUCTURE_SET_REGISTRY),
            rm.registryOrThrow(Registry.BIOME_REGISTRY),
            rm.registryOrThrow(Registry.NOISE_REGISTRY)
        );
    }
    
    public static ChunkGenerator createErrorTerrainGenerator(long seed, RegistryAccess rm) {
        return ErrorTerrainGenerator.create(
            rm.registryOrThrow(Registry.STRUCTURE_SET_REGISTRY),
            rm.registryOrThrow(Registry.BIOME_REGISTRY),
            rm.registryOrThrow(Registry.NOISE_REGISTRY)
        );
    }
    
    public static ChunkGenerator createVoidGenerator(RegistryAccess rm) {
        Registry<Biome> biomeRegistry = rm.registryOrThrow(Registry.BIOME_REGISTRY);
        
        Registry<StructureSet> structureSets = rm.registryOrThrow(Registry.STRUCTURE_SET_REGISTRY);
        
        FlatLevelGeneratorSettings flatChunkGeneratorConfig =
            new FlatLevelGeneratorSettings(Optional.empty(), biomeRegistry);
        flatChunkGeneratorConfig.getLayersInfo().add(new FlatLayerInfo(1, Blocks.AIR));
        flatChunkGeneratorConfig.updateLayers();
        
        return new FlatLevelSource(structureSets, flatChunkGeneratorConfig);
    }
    
    
    private static void tick() {
        if (!IPGlobal.enableAlternateDimensions) {
            return;
        }
        
        ServerLevel overworld = McHelper.getServerWorld(Level.OVERWORLD);
        
        MinecraftServer server = MiscHelper.getServer();
        
        syncWithOverworldTimeWeather(server.getLevel(alternate1), overworld);
        syncWithOverworldTimeWeather(server.getLevel(alternate2), overworld);
        syncWithOverworldTimeWeather(server.getLevel(alternate3), overworld);
        syncWithOverworldTimeWeather(server.getLevel(alternate4), overworld);
        syncWithOverworldTimeWeather(server.getLevel(alternate5), overworld);
    }
}
