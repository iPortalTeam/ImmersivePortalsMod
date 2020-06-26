package com.qouteall.immersive_portals.alternate_dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.Heightmap;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.VanillaLayeredBiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkRandom;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.ChunkGeneratorType;
import net.minecraft.world.gen.chunk.NoiseConfig;
import net.minecraft.world.gen.chunk.NoiseSamplingConfig;
import net.minecraft.world.gen.chunk.SlideConfig;
import net.minecraft.world.gen.chunk.StructuresConfig;
import net.minecraft.world.gen.chunk.SurfaceChunkGenerator;
import net.minecraft.world.gen.feature.FeatureConfig;
import net.minecraft.world.gen.feature.StructureFeature;

import java.util.List;
import java.util.Optional;

public class NormalSkylandGenerator extends ChunkGenerator {
    
    public static final Codec<NormalSkylandGenerator> codec = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.LONG.fieldOf("seed").stable().forGetter(g -> g.worldSeed)
        ).apply(instance, instance.stable(NormalSkylandGenerator::new))
    );
    
    private long worldSeed;
    private final SurfaceChunkGenerator proxy;
    
    public NormalSkylandGenerator(
        long seed
    ) {
        super(
            new VanillaLayeredBiomeSource(seed, false, false),
            new StructuresConfig(true)
        );
        
        worldSeed = seed;
        
        proxy = new SurfaceChunkGenerator(
            this.getBiomeSource(),
            seed,
            new ChunkGeneratorType.Preset("floating_islands", (preset) -> {
                return ChunkGeneratorType.Preset.createIslandsType(
                    new StructuresConfig(false),
                    Blocks.STONE.getDefaultState(),
                    Blocks.WATER.getDefaultState(),
                    preset,
                    false,
                    false
                );
            }).getChunkGeneratorType()
        );
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
        proxy.buildSurface(region, chunk);
    }
    
    @Override
    public void populateNoise(
        WorldAccess world, StructureAccessor accessor, Chunk chunk
    ) {
        proxy.populateNoise(world, accessor, chunk);
    }
    
    //make end city and woodland mansion be able to generate
    @Override
    public int getHeight(int x, int z, Heightmap.Type heightmapType) {
        return proxy.getHeight(x, z, heightmapType);
    }
    
    @Override
    public BlockView getColumnSample(int x, int z) {
        return proxy.getColumnSample(x, z);
    }
    
    @Override
    public int getMaxY() {
        return proxy.getMaxY();
    }
    
    @Override
    public int getSeaLevel() {
        return proxy.getSeaLevel();
    }
    
    @Override
    public List<Biome.SpawnEntry> getEntitySpawnList(Biome biome, StructureAccessor accessor, SpawnGroup group, BlockPos pos) {
        return proxy.getEntitySpawnList(biome, accessor, group, pos);
    }
    
    @Override
    public void populateEntities(ChunkRegion region) {
        proxy.populateEntities(region);
    }
    
}
