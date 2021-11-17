package qouteall.imm_ptl.peripheral.alternate_dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.dynamic.RegistryLookupCodec;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.SeedMixer;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;

import java.util.List;
import java.util.stream.Collectors;

public class ChaosBiomeSource extends BiomeSource {
    public static Codec<ChaosBiomeSource> codec = RecordCodecBuilder.create(instance -> instance.group(
        Codec.LONG.fieldOf("seed").forGetter(o -> o.worldSeed),
        RegistryLookupCodec.of(Registry.BIOME_KEY).forGetter(o -> o.biomeRegistry)
    ).apply(instance, instance.stable(ChaosBiomeSource::new)));
    
    private long worldSeed;
    private Registry<Biome> biomeRegistry;
    private List<Biome> biomes;
    
    public ChaosBiomeSource(long seed, Registry<Biome> biomeRegistry) {
        super(biomeRegistry.stream().collect(Collectors.toList()));
        
        worldSeed = seed;
        this.biomeRegistry = biomeRegistry;
        
        // java does not allow doing things before constructor
        biomes = biomeRegistry.stream().collect(Collectors.toList());
    }
    
    private Biome getRandomBiome(int x, int z) {
        int biomeNum = biomes.size();
        
        int index = (Math.abs((int) SeedMixer.mixSeed(x, z))) % biomeNum;
        return biomes.get(index);
    }

//    @Override
//    public Biome getBiomeForNoiseGen(int biomeX, int biomeY, int biomeZ) {
//        return VoronoiBiomeAccessType.INSTANCE.getBiome(
//            worldSeed, biomeX / 2, 0, biomeZ / 2,
//            (x, y, z) -> getRandomBiome(x, z)
//        );
//    }
    
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
