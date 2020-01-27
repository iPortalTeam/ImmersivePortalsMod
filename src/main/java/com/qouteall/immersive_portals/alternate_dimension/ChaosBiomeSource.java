package com.qouteall.immersive_portals.alternate_dimension;

import net.minecraft.util.math.noise.PerlinNoiseSampler;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biomes;
import net.minecraft.world.biome.source.BiomeSource;

import java.util.Arrays;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public class ChaosBiomeSource extends BiomeSource {
    private PerlinNoiseSampler sampler;
    private Biome[] biomeArray;
    
    public ChaosBiomeSource(long seed) {
        super(Registry.BIOME.stream().collect(Collectors.toSet()));
        
        sampler = new PerlinNoiseSampler(new Random(seed));
        
        Biome[] excluded = {
            Biomes.THE_END,
            Biomes.END_BARRENS,
            Biomes.END_HIGHLANDS,
            Biomes.END_MIDLANDS,
            Biomes.SMALL_END_ISLANDS
        };
        Set<Biome> excludedSet = Arrays.stream(excluded).collect(Collectors.toSet());
        
        biomeArray = Registry.BIOME.stream()
            .filter(biome -> !excludedSet.contains(biome))
            .toArray(Biome[]::new);
    }
    
    
    @Override
    public Biome getBiomeForNoiseGen(int biomeX, int biomeY, int biomeZ) {
        double sampled = sampler.sample(biomeX, 0, biomeZ, 0, 0);
        double whatever = sampled * 23333;
        double decimal = whatever - Math.floor(whatever);
        
        int biomeNum = biomeArray.length;
        int index = (int) Math.floor(decimal * biomeNum);
        if (index >= biomeNum) {
            index = biomeNum;
        }
        return biomeArray[index];
    }
}
