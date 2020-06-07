package com.qouteall.immersive_portals.alternate_dimension;

import net.minecraft.world.Heightmap;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.gen.chunk.ChunkGeneratorType;
import net.minecraft.world.gen.chunk.FloatingIslandsChunkGenerator;
import net.minecraft.world.gen.feature.FeatureConfig;
import net.minecraft.world.gen.feature.StructureFeature;

public class NormalSkylandGenerator extends FloatingIslandsChunkGenerator {
    
    
    public NormalSkylandGenerator(
        BiomeSource biomeSource,
        long seed,
        ChunkGeneratorType config
    ) {
        super(biomeSource, seed, config);
    }
    
    @Override
    public boolean hasStructure(
        Biome biome, StructureFeature<? extends FeatureConfig> structureFeature
    ) {
        if (structureFeature == StructureFeature.MINESHAFT) {
            //no mineshaft
            return false;
        }
        return super.hasStructure(biome, structureFeature);
    }
    
    //make end city and woodland mansion be able to generate
    @Override
    public int getHeightOnGround(int x, int z, Heightmap.Type heightmapType) {
        return 64;
    }
}
