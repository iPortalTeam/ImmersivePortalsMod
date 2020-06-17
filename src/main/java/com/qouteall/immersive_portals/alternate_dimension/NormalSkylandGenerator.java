package com.qouteall.immersive_portals.alternate_dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.BlockView;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.Heightmap;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.VanillaLayeredBiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.StructuresConfig;
import net.minecraft.world.gen.feature.FeatureConfig;
import net.minecraft.world.gen.feature.StructureFeature;

public class NormalSkylandGenerator extends ChunkGenerator {
    
    public static final Codec<NormalSkylandGenerator> codec = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.LONG.fieldOf("seed").stable().forGetter(
                g -> g.worldSeed
            )
        ).apply(
            instance,
            instance.stable(NormalSkylandGenerator::new)
        )
    );
    
    private long worldSeed;
    
    public NormalSkylandGenerator(
        long seed
    ) {
        super(
            new VanillaLayeredBiomeSource(seed, false, false),
            new StructuresConfig(true)
        );
        
        worldSeed = seed;
    }
    
    @Override
    protected Codec<? extends ChunkGenerator> method_28506() {
        return codec;
    }
    
    @Override
    public ChunkGenerator withSeed(long seed) {
        worldSeed = seed;
        return this;
    }
    
    @Override
    public void buildSurface(
        ChunkRegion region, Chunk chunk
    ) {
    
    }
    
    @Override
    public void populateNoise(
        WorldAccess world, StructureAccessor accessor, Chunk chunk
    ) {
    
    }
    
    @Override
    public int getHeight(int x, int z, Heightmap.Type heightmapType) {
        return 0;
    }
    
    @Override
    public BlockView getColumnSample(int x, int z) {
        return null;
    }
    
    //make end city and woodland mansion be able to generate
    @Override
    public int getHeightOnGround(int x, int z, Heightmap.Type heightmapType) {
        return 64;
    }
}
