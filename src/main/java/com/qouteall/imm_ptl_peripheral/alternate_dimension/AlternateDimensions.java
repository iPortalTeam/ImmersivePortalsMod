package com.qouteall.imm_ptl_peripheral.alternate_dimension;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.serialization.Lifecycle;
import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.ducks.IEWorld;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.util.registry.MutableRegistry;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.util.registry.SimpleRegistry;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.VanillaLayeredBiomeSource;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionType;
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
import java.util.function.Supplier;

public class AlternateDimensions {
    public static final RegistryKey<DimensionOptions> alternate1Option = RegistryKey.of(
        Registry.DIMENSION_OPTIONS,
        new Identifier("immersive_portals:alternate1")
    );
    public static final RegistryKey<DimensionOptions> alternate2Option = RegistryKey.of(
        Registry.DIMENSION_OPTIONS,
        new Identifier("immersive_portals:alternate2")
    );
    public static final RegistryKey<DimensionOptions> alternate3Option = RegistryKey.of(
        Registry.DIMENSION_OPTIONS,
        new Identifier("immersive_portals:alternate3")
    );
    public static final RegistryKey<DimensionOptions> alternate4Option = RegistryKey.of(
        Registry.DIMENSION_OPTIONS,
        new Identifier("immersive_portals:alternate4")
    );
    public static final RegistryKey<DimensionOptions> alternate5Option = RegistryKey.of(
        Registry.DIMENSION_OPTIONS,
        new Identifier("immersive_portals:alternate5")
    );
    public static final RegistryKey<DimensionType> surfaceType = RegistryKey.of(
        Registry.DIMENSION_TYPE_KEY,
        new Identifier("immersive_portals:surface_type")
    );
    public static final RegistryKey<World> alternate1 = RegistryKey.of(
        Registry.DIMENSION,
        new Identifier("immersive_portals:alternate1")
    );
    public static final RegistryKey<World> alternate2 = RegistryKey.of(
        Registry.DIMENSION,
        new Identifier("immersive_portals:alternate2")
    );
    public static final RegistryKey<World> alternate3 = RegistryKey.of(
        Registry.DIMENSION,
        new Identifier("immersive_portals:alternate3")
    );
    public static final RegistryKey<World> alternate4 = RegistryKey.of(
        Registry.DIMENSION,
        new Identifier("immersive_portals:alternate4")
    );
    public static final RegistryKey<World> alternate5 = RegistryKey.of(
        Registry.DIMENSION,
        new Identifier("immersive_portals:alternate5")
    );
    public static DimensionType surfaceTypeObject;
    
    public static boolean isAlternateDimension(World world) {
        final RegistryKey<World> key = world.getRegistryKey();
        return key == alternate1 ||
            key == alternate2 ||
            key == alternate3 ||
            key == alternate4 ||
            key == alternate5;
    }
    
    public static void init() {
        ModMain.postServerTickSignal.connect(() -> {
            if (!Global.enableAlternateDimensions) {
                return;
            }
            
            ServerWorld overworld = McHelper.getServerWorld(World.OVERWORLD);
            
            syncWithOverworldTimeWeather(McHelper.getServerWorld(alternate1), overworld);
            syncWithOverworldTimeWeather(McHelper.getServerWorld(alternate2), overworld);
            syncWithOverworldTimeWeather(McHelper.getServerWorld(alternate3), overworld);
            syncWithOverworldTimeWeather(McHelper.getServerWorld(alternate4), overworld);
            syncWithOverworldTimeWeather(McHelper.getServerWorld(alternate5), overworld);
        });
    }
    
    private static void syncWithOverworldTimeWeather(ServerWorld world, ServerWorld overworld) {
        ((IEWorld) world).portal_setWeather(
            overworld.getRainGradient(1), overworld.getRainGradient(1),
            overworld.getThunderGradient(1), overworld.getThunderGradient(1)
        );
    }
    
    public static ChunkGenerator createSkylandGenerator(long seed, DynamicRegistryManager rm) {
        
        MutableRegistry<Biome> biomeRegistry = rm.get(Registry.BIOME_KEY);
        VanillaLayeredBiomeSource biomeSource = new VanillaLayeredBiomeSource(
            seed, false, false, biomeRegistry
        );
        
        MutableRegistry<ChunkGeneratorSettings> settingsRegistry = rm.get(Registry.NOISE_SETTINGS_WORLDGEN);
        
        HashMap<StructureFeature<?>, StructureConfig> structureMap = new HashMap<>();
        structureMap.putAll(StructuresConfig.DEFAULT_STRUCTURES);
        structureMap.remove(StructureFeature.MINESHAFT);
        structureMap.remove(StructureFeature.STRONGHOLD);
        
        StructuresConfig structuresConfig = new StructuresConfig(
            Optional.empty(), structureMap
        );
        ChunkGeneratorSettings skylandSetting = ChunkGeneratorSettings.createIslandSettings(
            structuresConfig, Blocks.STONE.getDefaultState(),
            Blocks.WATER.getDefaultState(), new Identifier("imm_ptl:skyland_gen_id"),
            false, false
        );
        
        return new NoiseChunkGenerator(
            biomeSource, seed, () -> skylandSetting
        );
    }
    
    public static ChunkGenerator createErrorTerrainGenerator(long seed, DynamicRegistryManager rm) {
        MutableRegistry<Biome> biomeRegistry = rm.get(Registry.BIOME_KEY);
        
        ChaosBiomeSource chaosBiomeSource = new ChaosBiomeSource(seed, biomeRegistry);
        return new ErrorTerrainGenerator(seed, chaosBiomeSource);
    }
    
    public static ChunkGenerator createVoidGenerator(DynamicRegistryManager rm) {
        MutableRegistry<Biome> biomeRegistry = rm.get(Registry.BIOME_KEY);
        
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
    
    public static void addDimension(
        long argSeed,
        SimpleRegistry<DimensionOptions> registry,
        RegistryKey<DimensionOptions> key,
        Supplier<DimensionType> dimensionTypeSupplier,
        ChunkGenerator chunkGenerator
    ) {
        if (!registry.getIds().contains(key.getValue())) {
            registry.add(
                key,
                new DimensionOptions(
                    dimensionTypeSupplier,
                    chunkGenerator
                ),
                Lifecycle.experimental()
            );
        }
    }
    
    public static void addAlternateDimensions(
        SimpleRegistry<DimensionOptions> registry, DynamicRegistryManager rm,
        long seed
    ) {
        if (!Global.enableAlternateDimensions) {
            return;
        }
        
        addDimension(
            seed,
            registry,
            alternate1Option,
            () -> surfaceTypeObject,
            createSkylandGenerator(seed, rm)
        );
        
        addDimension(
            seed,
            registry,
            alternate2Option,
            () -> surfaceTypeObject,
            createSkylandGenerator(seed, rm)
        );
        
        addDimension(
            seed,
            registry,
            alternate3Option,
            () -> surfaceTypeObject,
            createErrorTerrainGenerator(seed, rm)
        );
        
        addDimension(
            seed,
            registry,
            alternate4Option,
            () -> surfaceTypeObject,
            createErrorTerrainGenerator(seed, rm)
        );
        
        addDimension(
            seed,
            registry,
            alternate5Option,
            () -> surfaceTypeObject,
            createVoidGenerator(rm)
        );
    }
    
    // don't store dimension info into level.dat
    // avoid weird dfu error
    public static SimpleRegistry<DimensionOptions> getAlternateDimensionsRemoved(
        SimpleRegistry<DimensionOptions> registry
    ) {
        return McHelper.filterAndCopyRegistry(
            registry,
            (key, obj) -> !(key == alternate1Option ||
                key == alternate2Option ||
                key == alternate3Option ||
                key == alternate4Option ||
                key == alternate5Option)
        );
    }
    
    // When DFU does not recognize a mod dimension (in level.dat) it will throw an error
    // then the nether and the end will be swallowed
    // it's not IP's issue. but I add the fix code because many people encounter the issue
    public static void addMissingVanillaDimensions(
        SimpleRegistry<DimensionOptions> registry, DynamicRegistryManager rm,
        long seed
    ) {
        if (!registry.getIds().contains(DimensionOptions.NETHER.getValue())) {
            Helper.err("Missing the nether. This may be caused by DFU. Trying to fix");
            
            addDimension(
                seed,
                registry,
                DimensionOptions.NETHER,
                () -> DimensionType.THE_NETHER,
                DimensionType.createNetherGenerator(
                    rm.get(Registry.BIOME_KEY),
                    rm.get(Registry.NOISE_SETTINGS_WORLDGEN),
                    seed
                )
            );
        }
        
        if (!registry.getIds().contains(DimensionOptions.END.getValue())) {
            Helper.err("Missing the end. This may be caused by DFU. Trying to fix");
            addDimension(
                seed,
                registry,
                DimensionOptions.END,
                () -> DimensionType.THE_END,
                DimensionType.createEndGenerator(
                    rm.get(Registry.BIOME_KEY),
                    rm.get(Registry.NOISE_SETTINGS_WORLDGEN),
                    seed
                )
            );
        }
    }
    
}
