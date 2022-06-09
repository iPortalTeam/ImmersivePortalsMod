package qouteall.imm_ptl.peripheral.alternate_dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.data.worldgen.SurfaceRuleData;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import qouteall.imm_ptl.peripheral.mixin.common.alternate_dimension.IEChunk1;
import qouteall.imm_ptl.peripheral.mixin.common.alternate_dimension.IENoiseGeneratorSettings;
import qouteall.imm_ptl.peripheral.mixin.common.alternate_dimension.IENoiseRouterData;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

// inherit NoiseBasedChunkGenerator because I want to use my custom codec
public class NormalSkylandGenerator extends NoiseBasedChunkGenerator {
    
    public static final Codec<NormalSkylandGenerator> codec = RecordCodecBuilder.create(
        instance -> instance.group(
                RegistryOps.retrieveRegistry(Registry.STRUCTURE_SET_REGISTRY).forGetter(g -> g.structureSets),
                RegistryOps.retrieveRegistry(Registry.BIOME_REGISTRY).forGetter(g -> g.biomeRegistry),
                RegistryOps.retrieveRegistry(Registry.NOISE_REGISTRY).forGetter(g -> g.noiseRegistry),
                RegistryOps.retrieveRegistry(Registry.NOISE_GENERATOR_SETTINGS_REGISTRY).forGetter(g -> g.noiseGeneratorSettingsRegistry)
            )
            .apply(instance, NormalSkylandGenerator::create)
    );
    
    public NormalSkylandGenerator(
        Registry<StructureSet> structureSets,
        Registry<NormalNoise.NoiseParameters> noiseParametersRegistry,
        BiomeSource biomeSource,
        Holder<NoiseGeneratorSettings> noiseGeneratorSettings,

        Registry<Biome> biomeRegistry,
        Registry<NoiseGeneratorSettings> noiseGeneratorSettingsRegistry,

        NoiseBasedChunkGenerator delegate,
        NoiseGeneratorSettings delegateNGSettings
    ) {
        super(structureSets, noiseParametersRegistry, biomeSource, noiseGeneratorSettings);
        
        this.biomeRegistry = biomeRegistry;
        this.noiseRegistry = noiseParametersRegistry;
        this.noiseGeneratorSettingsRegistry = noiseGeneratorSettingsRegistry;
        this.delegate = delegate;
        this.delegateNGSettings = delegateNGSettings;
    }
    
    public static NormalSkylandGenerator create(
        Registry<StructureSet> structureSets, Registry<Biome> biomeRegistry,
        Registry<NormalNoise.NoiseParameters> noiseRegistry,
        Registry<NoiseGeneratorSettings> noiseGeneratorSettingsRegistry
    ) {
        MultiNoiseBiomeSource overworldBiomeSource = MultiNoiseBiomeSource.Preset.OVERWORLD.biomeSource(biomeRegistry);
        Set<Holder<Biome>> overworldBiomes = overworldBiomeSource.possibleBiomes();
        
        BiomeSource chaosBiomeSource = new ChaosBiomeSource(HolderSet.direct(new ArrayList<>(overworldBiomes)));
        
        NoiseGeneratorSettings overworldNGSettings = IENoiseGeneratorSettings.ip_overworld(false, false);
        
        NoiseGeneratorSettings skylandNGSettings = IENoiseGeneratorSettings.ip_floatingIslands();
        
        NoiseBasedChunkGenerator skylandChunkGenerator = new NoiseBasedChunkGenerator(
            structureSets, noiseRegistry,
            overworldBiomeSource, Holder.direct(skylandNGSettings)
        );
        
        NoiseBasedChunkGenerator overworldGenerator = new NoiseBasedChunkGenerator(
            structureSets, noiseRegistry,
            overworldBiomeSource, Holder.direct(overworldNGSettings)
        );
        
        return new NormalSkylandGenerator(
            structureSets,
            noiseRegistry,
            overworldBiomeSource,
            Holder.direct(skylandNGSettings),
            biomeRegistry,
            noiseGeneratorSettingsRegistry,
            overworldGenerator,
            overworldNGSettings
        );
    }
    
    public final Registry<Biome> biomeRegistry;
    public final Registry<NormalNoise.NoiseParameters> noiseRegistry;
    public final Registry<NoiseGeneratorSettings> noiseGeneratorSettingsRegistry;
    private final NoiseBasedChunkGenerator delegate;
    private final NoiseGeneratorSettings delegateNGSettings;
    
    @Override
    protected Codec<? extends ChunkGenerator> codec() {
        return codec;
    }
}
