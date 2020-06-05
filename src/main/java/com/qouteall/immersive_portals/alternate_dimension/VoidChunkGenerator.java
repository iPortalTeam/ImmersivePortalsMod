package com.qouteall.immersive_portals.alternate_dimension;

import net.minecraft.class_5284;
import net.minecraft.structure.StructureManager;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.FloatingIslandsChunkGenerator;
import net.minecraft.world.gen.feature.FeatureConfig;
import net.minecraft.world.gen.feature.StructureFeature;

public class VoidChunkGenerator extends FloatingIslandsChunkGenerator {
    
    
    public VoidChunkGenerator(
        BiomeSource biomeSource,
        long seed,
        class_5284 config
    ) {
        super(biomeSource, seed, config);
    }
    
    @Override
    public void populateNoise(WorldAccess world, StructureAccessor structureAccessor, Chunk chunk) {
        //nothing
    }
    
    @Override
    public boolean hasStructure(
        Biome biome, StructureFeature<? extends FeatureConfig> structureFeature
    ) {
        return false;
    }
    
    @Override
    public void setStructureStarts(
        StructureAccessor accessor,
        BiomeAccess biomeAccess,
        Chunk chunk,
        ChunkGenerator generator,
        StructureManager manager,
        long seed
    ) {
        //nothing
    }
}
