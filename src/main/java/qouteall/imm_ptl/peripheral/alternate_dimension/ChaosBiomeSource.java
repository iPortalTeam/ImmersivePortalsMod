package qouteall.imm_ptl.peripheral.alternate_dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.dynamic.RegistryLookupCodec;
import net.minecraft.util.registry.BuiltinRegistries;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.MultiNoiseBiomeSource;
import net.minecraft.world.biome.source.SeedMixer;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class ChaosBiomeSource extends BiomeSource {
    public static Codec<ChaosBiomeSource> codec = RecordCodecBuilder.create(instance -> instance.group(
        Codec.LONG.fieldOf("seed").forGetter(o -> o.worldSeed),
        RegistryLookupCodec.of(Registry.BIOME_KEY).forGetter(o -> o.biomeRegistry)
    ).apply(instance, instance.stable(ChaosBiomeSource::new)));
    
    private long worldSeed;
    private Registry<Biome> biomeRegistry;
    private List<Biome> biomes;
    
    private static List<RegistryKey<Biome>> getOverworldBiomeIds() {
        Set<Biome> set = MultiNoiseBiomeSource.Preset.OVERWORLD.getBiomeSource(BuiltinRegistries.BIOME).getBiomes();
        return set.stream().map(BuiltinRegistries.BIOME::getKey).flatMap(Optional::stream).collect(Collectors.toList());
    }
    
    public static List<Biome> getInvolvedBiomes(Registry<Biome> biomeRegistry) {
        
        return getOverworldBiomeIds().stream().flatMap(
            biomeRegistryKey -> biomeRegistry.getOrEmpty(biomeRegistryKey).stream()
        ).collect(Collectors.toList());


//        return biomeRegistry.getIds().stream().filter(
//            id -> id.getNamespace().equals("minecraft")
//        ).map(id -> biomeRegistry.get(id)).toList();
    }
    
    public ChaosBiomeSource(long seed, Registry<Biome> biomeRegistry) {
        super(getInvolvedBiomes(biomeRegistry));
        
        worldSeed = seed;
        this.biomeRegistry = biomeRegistry;
        
        // java does not allow doing things before constructor, so invoke this again
        biomes = getInvolvedBiomes(biomeRegistry);
    }
    
    private Biome getRandomBiome(int x, int z) {
        int biomeNum = biomes.size();
        
        int index = (Math.abs((int) SeedMixer.mixSeed(x / 5, z / 5))) % biomeNum;
        return biomes.get(index);
    }
    
    @Override
    protected Codec<? extends BiomeSource> getCodec() {
        return codec;
    }
    
    @Override
    public BiomeSource withSeed(long seed) {
        return new ChaosBiomeSource(seed, biomeRegistry);
    }
    
    @Override
    public Biome getBiome(int i, int j, int k, MultiNoiseUtil.MultiNoiseSampler multiNoiseSampler) {
        return getRandomBiome(i, i + j + k);
    }
}
