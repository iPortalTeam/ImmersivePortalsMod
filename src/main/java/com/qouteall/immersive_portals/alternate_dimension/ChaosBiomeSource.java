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
    
    private double sampleValue(int x, int y, int z) {
        int scale = 7;
        
        int x1 = Math.floorDiv(x, scale * scale * scale);
        int y1 = Math.floorDiv(y, scale * scale * scale);
        int z1 = Math.floorDiv(z, scale * scale * scale);
        int x2 = Math.floorDiv(x - x1, scale * scale);
        int y2 = Math.floorDiv(y - y1, scale * scale);
        int z2 = Math.floorDiv(z - z1, scale * scale);
        int x3 = Math.floorDiv(x - x2, scale * scale);
        int y3 = Math.floorDiv(y - y2, scale * scale);
        int z3 = Math.floorDiv(z - z2, scale * scale);
        
        double d2 = scale * scale;
        double d3 = scale;
        
        return sampler.sample(
            x1, y1, z1,
            x2 / d2, y2 / d2, z2 / d2,
            x3 / d3, y3 / d3, z3 / d3
        );
    }
    
    @Override
    public Biome getBiomeForNoiseGen(int biomeX, int biomeY, int biomeZ) {
//        Helper.log(String.format("%s %s %s", biomeX, biomeY, biomeZ));

//        double sampled = sampler.sample(biomeX, 0, biomeZ, 0, 0);
        double sampled = sampleValue(biomeX, biomeY, biomeZ);
        double whatever = sampled * 53;
        double decimal = whatever - Math.floor(whatever);
        
        int biomeNum = biomeArray.length;
        int index = (int) Math.floor(decimal * biomeNum);
        if (index >= biomeNum) {
            index = biomeNum;
        }
        return biomeArray[index];
    }
}
