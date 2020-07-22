package com.qouteall.immersive_portals.alternate_dimension;

import com.mojang.serialization.Codec;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.SeedMixer;
import net.minecraft.world.biome.source.VoronoiBiomeAccessType;

import java.util.stream.Collectors;

public class ChaosBiomeSource extends BiomeSource {
    private Biome[] biomeArray;
    private long worldSeed;
    
    public ChaosBiomeSource(long seed) {
        super(Registry.BIOME.stream().collect(Collectors.toList()));
        
        worldSeed = seed;
        
        biomeArray = Registry.BIOME.stream()
            .toArray(Biome[]::new);
    }
    
    private Biome getRandomBiome(int x, int z) {
        int biomeNum = biomeArray.length;
        
        int index = (Math.abs((int) SeedMixer.mixSeed(x, z))) % biomeNum;
        return biomeArray[(int) index];
    }
    
    @Override
    public Biome getBiomeForNoiseGen(int biomeX, int biomeY, int biomeZ) {
        return VoronoiBiomeAccessType.INSTANCE.getBiome(
            worldSeed, biomeX / 2, 0, biomeZ / 2,
            (x, y, z) -> getRandomBiome(x, z)
        );
    }
    
    @Override
    protected Codec<? extends BiomeSource> method_28442() {
        return null;
    }
    
    @Override
    public BiomeSource withSeed(long seed) {
        worldSeed = seed;
        return this;
    }
}
