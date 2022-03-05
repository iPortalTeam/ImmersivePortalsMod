package qouteall.imm_ptl.peripheral.alternate_dimension;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import qouteall.imm_ptl.peripheral.mixin.common.alternate_dimension.IEChunk1;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class DelegatedChunkGenerator extends ChunkGenerator {
    
    protected ChunkGenerator delegate;
    protected BiomeSource biomeSource_;
    
    public DelegatedChunkGenerator(
        Registry<StructureSet> structureSets,
        Optional<HolderSet<StructureSet>> structureOverrides,
        BiomeSource biomeSource,
        ChunkGenerator delegate
    ) {
        super(structureSets, structureOverrides, biomeSource);
        this.delegate = delegate;
        this.biomeSource_ = biomeSource;
    }
    
    @Override
    protected Codec<? extends ChunkGenerator> codec() {
        return NoiseBasedChunkGenerator.CODEC;
    }
    
    @Override
    public ChunkGenerator withSeed(long seed) {
        return new DelegatedChunkGenerator(
            structureSets,
            structureOverrides,
            runtimeBiomeSource.withSeed(seed),
            delegate.withSeed(seed)
        );
    }
    
    @Override
    public Climate.Sampler climateSampler() {
        if (delegate == null) {
            return null;
        }
        return delegate.climateSampler();
    }
    
    @Override
    public void applyCarvers(WorldGenRegion chunkRegion, long seed, BiomeManager biomeAccess, StructureFeatureManager structureAccessor, ChunkAccess chunk, GenerationStep.Carving generationStep) {
        delegate.applyCarvers(chunkRegion, seed, biomeAccess, structureAccessor, chunk, generationStep);
    }
    
    @Override
    public void buildSurface(WorldGenRegion region, StructureFeatureManager structures, ChunkAccess chunk) {
        ((IEChunk1) chunk).ip_setChunkNoiseSampler(null);
        
        delegate.buildSurface(region, structures, chunk);
    }
    
    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
        delegate.spawnOriginalMobs(region);
    }
    
    @Override
    public int getGenDepth() {
        return delegate.getGenDepth();
    }
    
    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender arg, StructureFeatureManager structureAccessor, ChunkAccess chunk) {
        return delegate.fillFromNoise(executor, arg, structureAccessor, chunk);
    }
    
    @Override
    public int getSeaLevel() {
        return delegate.getSeaLevel();
    }
    
    @Override
    public int getMinY() {
        return delegate.getMinY();
    }
    
    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types heightmap, LevelHeightAccessor world) {
        return delegate.getBaseHeight(x, z, heightmap, world);
    }
    
    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor world) {
        return delegate.getBaseColumn(x, z, world);
    }
    
    @Override
    public void addDebugScreenInfo(List<String> list, BlockPos blockPos) {
    
    }

//    public static class SpecialNoise extends DelegatedChunkGenerator {
//
//        public final ChunkGenerator noiseDelegate;
//
//        public SpecialNoise(
//            BiomeSource biomeSource, StructureSettings structuresConfig,
//            ChunkGenerator delegate,
//            ChunkGenerator noiseDelegate
//        ) {
//            super(biomeSource, structuresConfig, delegate);
//            this.noiseDelegate = noiseDelegate;
//        }
//
//        @Override
//        public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender arg, StructureFeatureManager structureAccessor, ChunkAccess chunk) {
//            return noiseDelegate.fillFromNoise(executor, arg, structureAccessor, chunk);
//        }
//    }
}
