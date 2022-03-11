package qouteall.imm_ptl.peripheral.alternate_dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.data.worldgen.TerrainProvider;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseSamplingSettings;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.NoiseSlider;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import qouteall.imm_ptl.peripheral.mixin.common.alternate_dimension.IENoiseGeneratorSettings;

import java.util.List;
import java.util.stream.Collectors;

public class NormalSkylandGenerator extends DelegatedChunkGenerator {
    
    public static final Codec<NormalSkylandGenerator> codec = RecordCodecBuilder.create(
        instance -> instance.group(
                Codec.LONG.fieldOf("seed").stable().forGetter(g -> g.seed),
                
                RegistryOps.retrieveRegistry(Registry.STRUCTURE_SET_REGISTRY).forGetter(g -> g.structureSets),
                RegistryOps.retrieveRegistry(Registry.BIOME_REGISTRY).forGetter(g -> g.biomeRegistry),
                RegistryOps.retrieveRegistry(Registry.NOISE_REGISTRY).forGetter(g -> g.noiseRegistry)
            )
            .apply(instance, NormalSkylandGenerator::create)
    );
    
    public static NormalSkylandGenerator create(
        Long seed, Registry<StructureSet> structureSets, Registry<Biome> biomeRegistry,
        Registry<NormalNoise.NoiseParameters> noiseRegistry
    ) {
        MultiNoiseBiomeSource biomeSource = MultiNoiseBiomeSource.Preset.OVERWORLD.biomeSource(
            biomeRegistry, true
        );
        
        NoiseGeneratorSettings skylandSetting = IENoiseGeneratorSettings.ip_floatingIslands();
        
        NoiseBasedChunkGenerator islandChunkGenerator = new NoiseBasedChunkGenerator(
            structureSets, noiseRegistry,
            biomeSource, seed, Holder.direct(skylandSetting)
        );
        
        return new NormalSkylandGenerator(
            seed, structureSets, biomeSource, islandChunkGenerator,
            biomeRegistry, noiseRegistry
        );
    }
    
    public final long seed;
    public final Registry<Biome> biomeRegistry;
    public final Registry<NormalNoise.NoiseParameters> noiseRegistry;
    
    public NormalSkylandGenerator(
        long seed,
        Registry<StructureSet> structureSets,
        BiomeSource biomeSource, ChunkGenerator delegate,
        Registry<Biome> biomeRegistry,
        Registry<NormalNoise.NoiseParameters> noiseRegistry
    ) {
        super(structureSets, biomeSource, delegate);
        this.seed = seed;
        this.biomeRegistry = biomeRegistry;
        this.noiseRegistry = noiseRegistry;
    }
    
    @Override
    protected Codec<? extends ChunkGenerator> codec() {
        return codec;
    }
    
    @Override
    public ChunkGenerator withSeed(long seed) {
        return new NormalSkylandGenerator(
            seed, structureSets, biomeSource.withSeed(seed),
            delegate.withSeed(seed), biomeRegistry, noiseRegistry
        );
    }
}
