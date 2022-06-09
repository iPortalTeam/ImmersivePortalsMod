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
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import qouteall.imm_ptl.peripheral.mixin.common.alternate_dimension.IENoiseGeneratorSettings;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
        Registry<NoiseGeneratorSettings> noiseGeneratorSettingsRegistry
    ) {
        super(structureSets, noiseParametersRegistry, biomeSource, noiseGeneratorSettings);
        
        this.biomeRegistry = biomeRegistry;
        this.noiseRegistry = noiseParametersRegistry;
        this.noiseGeneratorSettingsRegistry = noiseGeneratorSettingsRegistry;
    }
    
    public static NormalSkylandGenerator create(
        Registry<StructureSet> structureSets, Registry<Biome> biomeRegistry,
        Registry<NormalNoise.NoiseParameters> noiseRegistry,
        Registry<NoiseGeneratorSettings> noiseGeneratorSettingsRegistry
    ) {
        MultiNoiseBiomeSource biomeSource = MultiNoiseBiomeSource.Preset.OVERWORLD.biomeSource(
            biomeRegistry
        );
        
        NoiseGeneratorSettings skylandSetting = IENoiseGeneratorSettings.ip_floatingIslands();
        
        // vanilla copy
        final NoiseSettings END_NOISE_SETTINGS = NoiseSettings.create(0, 128, 2, 1);

        // replace the noise setting
        skylandSetting = new NoiseGeneratorSettings(
            END_NOISE_SETTINGS,
            skylandSetting.defaultBlock(), skylandSetting.defaultFluid(),
            skylandSetting.noiseRouter(), skylandSetting.surfaceRule(),
            skylandSetting.spawnTarget(),
            skylandSetting.seaLevel(),
            skylandSetting.disableMobGeneration(), skylandSetting.aquifersEnabled(),
            skylandSetting.oreVeinsEnabled(), skylandSetting.useLegacyRandomSource()
        );
        
        Holder<NoiseGeneratorSettings> noiseGeneratorSettingsHolder =
            Holder.direct(skylandSetting);
        
        NoiseBasedChunkGenerator islandChunkGenerator = new NoiseBasedChunkGenerator(
            structureSets, noiseRegistry,
            biomeSource, noiseGeneratorSettingsHolder
        );
        
        return new NormalSkylandGenerator(
            structureSets,
            noiseRegistry,
            biomeSource,
            noiseGeneratorSettingsHolder,
            biomeRegistry,
            noiseGeneratorSettingsRegistry
        );
    }
    
    public final Registry<Biome> biomeRegistry;
    public final Registry<NormalNoise.NoiseParameters> noiseRegistry;
    public final Registry<NoiseGeneratorSettings> noiseGeneratorSettingsRegistry;
    
    @Override
    protected Codec<? extends ChunkGenerator> codec() {
        return codec;
    }
}
