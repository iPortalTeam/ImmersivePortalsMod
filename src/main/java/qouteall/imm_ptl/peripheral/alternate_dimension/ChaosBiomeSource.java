package qouteall.imm_ptl.peripheral.alternate_dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Registry;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.resources.RegistryLookupCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.LinearCongruentialGenerator;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class ChaosBiomeSource extends BiomeSource {
    public static Codec<ChaosBiomeSource> codec = RecordCodecBuilder.create(instance -> instance.group(
        Codec.LONG.fieldOf("seed").forGetter(o -> o.worldSeed),
        RegistryLookupCodec.create(Registry.BIOME_REGISTRY).forGetter(o -> o.biomeRegistry)
    ).apply(instance, instance.stable(ChaosBiomeSource::new)));
    
    private long worldSeed;
    private Registry<Biome> biomeRegistry;
    private List<Biome> biomes;
    
    private static List<ResourceKey<Biome>> getOverworldBiomeIds() {
        Set<Biome> set = MultiNoiseBiomeSource.Preset.OVERWORLD.biomeSource(BuiltinRegistries.BIOME).possibleBiomes();
        return set.stream().map(BuiltinRegistries.BIOME::getResourceKey).flatMap(Optional::stream).collect(Collectors.toList());
    }
    
    public static List<Biome> getInvolvedBiomes(Registry<Biome> biomeRegistry) {
        
        return getOverworldBiomeIds().stream().flatMap(
            biomeRegistryKey -> biomeRegistry.getOptional(biomeRegistryKey).stream()
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
        
        int index = (Math.abs((int) LinearCongruentialGenerator.next(x / 5, z / 5))) % biomeNum;
        return biomes.get(index);
    }
    
    @Override
    protected Codec<? extends BiomeSource> codec() {
        return codec;
    }
    
    @Override
    public BiomeSource withSeed(long seed) {
        return new ChaosBiomeSource(seed, biomeRegistry);
    }
    
    @Override
    public Biome getNoiseBiome(int i, int j, int k, Climate.Sampler multiNoiseSampler) {
        return getRandomBiome(i, i + j + k);
    }
}
