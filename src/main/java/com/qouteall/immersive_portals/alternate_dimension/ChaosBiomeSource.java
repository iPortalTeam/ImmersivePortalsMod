package com.qouteall.immersive_portals.alternate_dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryLookupCodec;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.SeedMixer;
import net.minecraft.world.biome.source.VoronoiBiomeAccessType;

import java.util.stream.Collectors;

public class ChaosBiomeSource extends BiomeSource {
    public static Codec<ChaosBiomeSource> codec = RecordCodecBuilder.create(instance -> instance.group(
        Codec.LONG.fieldOf("seed").forGetter(o -> o.worldSeed),
        RegistryLookupCodec.of(Registry.BIOME_KEY).forGetter(o -> o.biomeRegistry)
    ).apply(instance, instance.stable(ChaosBiomeSource::new)));
    
    private long worldSeed;
    private Registry<Biome> biomeRegistry;
    
    public ChaosBiomeSource(long seed, Registry<Biome> biomeRegistry) {
        super(biomeRegistry.stream().collect(Collectors.toList()));
        
        worldSeed = seed;
        this.biomeRegistry = biomeRegistry;
    }
    
    private Biome getRandomBiome(int x, int z) {
        int biomeNum = biomes.size();
        
        int index = (Math.abs((int) SeedMixer.mixSeed(x, z))) % biomeNum;
        return biomes.get(index);
    }
    
    @Override
    public Biome getBiomeForNoiseGen(int biomeX, int biomeY, int biomeZ) {
        return VoronoiBiomeAccessType.INSTANCE.getBiome(
            worldSeed, biomeX / 2, 0, biomeZ / 2,
            (x, y, z) -> getRandomBiome(x, z)
        );
    }
    
    @Override
    protected Codec<? extends BiomeSource> getCodec() {
        return codec;
    }
    
    @Override
    public BiomeSource withSeed(long seed) {
        worldSeed = seed;
        return this;
    }
}
