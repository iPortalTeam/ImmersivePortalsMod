package com.qouteall.immersive_portals.alternate_dimension;

import net.minecraft.world.IWorld;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.chunk.FloatingIslandsChunkGenerator;
import net.minecraft.world.gen.chunk.FloatingIslandsChunkGeneratorConfig;

public class VoidChunkGenerator extends FloatingIslandsChunkGenerator {
    public VoidChunkGenerator(
        IWorld iWorld,
        BiomeSource biomeSource,
        FloatingIslandsChunkGeneratorConfig floatingIslandsChunkGeneratorConfig
    ) {
        super(iWorld, biomeSource, floatingIslandsChunkGeneratorConfig);
    }
    
    @Override
    public void populateNoise(IWorld world, Chunk chunk) {
        //nothing
    }
}
