package com.qouteall.immersive_portals.alternate_dimension;

import net.minecraft.structure.StructureManager;
import net.minecraft.world.IWorld;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.FloatingIslandsChunkGenerator;
import net.minecraft.world.gen.chunk.FloatingIslandsChunkGeneratorConfig;
import net.minecraft.world.gen.feature.FeatureConfig;
import net.minecraft.world.gen.feature.StructureFeature;

public class VoidChunkGenerator extends FloatingIslandsChunkGenerator {
    public VoidChunkGenerator(
        IWorld iWorld,
        BiomeSource biomeSource,
        FloatingIslandsChunkGeneratorConfig floatingIslandsChunkGeneratorConfig
    ) {
        super(iWorld, biomeSource, floatingIslandsChunkGeneratorConfig);
    }
    
    @Override
    public void populateNoise(IWorld world, StructureAccessor structureAccessor, Chunk chunk) {
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
        StructureAccessor structureAccessor,
        BiomeAccess biomeAccess,
        Chunk chunk,
        ChunkGenerator<?> chunkGenerator,
        StructureManager structureManager
    ) {
        //nothing
    }
}
