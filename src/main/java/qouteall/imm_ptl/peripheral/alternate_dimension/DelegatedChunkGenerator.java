package qouteall.imm_ptl.peripheral.alternate_dimension;

import com.mojang.serialization.Codec;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
import net.minecraft.world.gen.chunk.StructuresConfig;
import net.minecraft.world.gen.chunk.VerticalBlockSample;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class DelegatedChunkGenerator extends ChunkGenerator {
    
    protected ChunkGenerator delegate;
    protected BiomeSource biomeSource_;
    
    public DelegatedChunkGenerator(BiomeSource biomeSource, StructuresConfig structuresConfig, ChunkGenerator delegate) {
        super(biomeSource, structuresConfig);
        this.delegate = delegate;
        this.biomeSource_ = biomeSource;
    }
    
    @Override
    protected Codec<? extends ChunkGenerator> getCodec() {
        return NoiseChunkGenerator.CODEC;
    }
    
    @Override
    public ChunkGenerator withSeed(long seed) {
        return new DelegatedChunkGenerator(
            biomeSource.withSeed(seed),
            getStructuresConfig(),
            delegate.withSeed(seed)
        );
    }
    
    @Override
    public MultiNoiseUtil.MultiNoiseSampler getMultiNoiseSampler() {
        return delegate.getMultiNoiseSampler();
    }
    
    @Override
    public void carve(ChunkRegion chunkRegion, long seed, BiomeAccess biomeAccess, StructureAccessor structureAccessor, Chunk chunk, GenerationStep.Carver generationStep) {
        delegate.carve(chunkRegion, seed, biomeAccess, structureAccessor, chunk, generationStep);
    }
    
    @Override
    public void buildSurface(ChunkRegion region, StructureAccessor structures, Chunk chunk) {
        delegate.buildSurface(region, structures, chunk);
    }
    
    @Override
    public void populateEntities(ChunkRegion region) {
        delegate.populateEntities(region);
    }
    
    @Override
    public int getWorldHeight() {
        return delegate.getWorldHeight();
    }
    
    @Override
    public CompletableFuture<Chunk> populateNoise(Executor executor, Blender arg, StructureAccessor structureAccessor, Chunk chunk) {
        return delegate.populateNoise(executor, arg, structureAccessor, chunk);
    }
    
    @Override
    public int getSeaLevel() {
        return delegate.getSeaLevel();
    }
    
    @Override
    public int getMinimumY() {
        return delegate.getMinimumY();
    }
    
    @Override
    public int getHeight(int x, int z, Heightmap.Type heightmap, HeightLimitView world) {
        return delegate.getHeight(x, z, heightmap, world);
    }
    
    @Override
    public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world) {
        return delegate.getColumnSample(x, z, world);
    }
    
    public static class SpecialNoise extends DelegatedChunkGenerator{
        
        public final ChunkGenerator noiseDelegate;
    
        public SpecialNoise(
            BiomeSource biomeSource, StructuresConfig structuresConfig,
            ChunkGenerator delegate,
            ChunkGenerator noiseDelegate
        ) {
            super(biomeSource, structuresConfig, delegate);
            this.noiseDelegate = noiseDelegate;
        }
    
        @Override
        public CompletableFuture<Chunk> populateNoise(Executor executor, Blender arg, StructureAccessor structureAccessor, Chunk chunk) {
//            return delegate.populateNoise(executor, arg, structureAccessor, chunk).thenComposeAsync(
//                chunk1 -> {
//                    for (int y = 0; y < 100; y++) {
//                        for (int x = 0; x < 16; x++) {
//                            for (int z = 0; z < 16; z++) {
//                                chunk1.setBlockState(new BlockPos(x, y, z), Blocks.AIR.getDefaultState(), false);
//                            }
//                        }
//                    }
//                    return noiseDelegate.populateNoise(executor, arg, structureAccessor, chunk);
//                }, executor
//            );
            return noiseDelegate.populateNoise(executor, arg, structureAccessor, chunk);
        }
    }
}
