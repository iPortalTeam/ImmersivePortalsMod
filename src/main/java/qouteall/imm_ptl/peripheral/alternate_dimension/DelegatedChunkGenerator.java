package qouteall.imm_ptl.peripheral.alternate_dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import qouteall.imm_ptl.peripheral.mixin.common.alternate_dimension.IEChunk1;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public abstract class DelegatedChunkGenerator extends ChunkGenerator {
    
    protected ChunkGenerator delegate;
    protected BiomeSource biomeSource_;
    
    public DelegatedChunkGenerator(
        Registry<StructureSet> structureSets,
        BiomeSource biomeSource,
        ChunkGenerator delegate
    ) {
        super(structureSets, Optional.empty(), biomeSource);
        this.delegate = delegate;
        this.biomeSource_ = biomeSource;
    }
    
    @Override
    public void applyCarvers(WorldGenRegion worldGenRegion, long l, RandomState randomState, BiomeManager biomeManager, StructureManager structureManager, ChunkAccess chunkAccess, GenerationStep.Carving carving) {
        delegate.applyCarvers(worldGenRegion, l, randomState, biomeManager, structureManager, chunkAccess, carving);
    }
    
    @Override
    public void buildSurface(WorldGenRegion worldGenRegion, StructureManager structureManager, RandomState randomState, ChunkAccess chunkAccess) {
        delegate.buildSurface(worldGenRegion, structureManager, randomState, chunkAccess);
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
    public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender, RandomState randomState, StructureManager structureManager, ChunkAccess chunkAccess) {
        return delegate.fillFromNoise(executor, blender, randomState, structureManager, chunkAccess);
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
    public int getBaseHeight(int i, int j, Heightmap.Types types, LevelHeightAccessor levelHeightAccessor, RandomState randomState) {
        return delegate.getBaseHeight(i, j, types, levelHeightAccessor, randomState);
    }
    
    @Override
    public NoiseColumn getBaseColumn(int i, int j, LevelHeightAccessor levelHeightAccessor, RandomState randomState) {
        return delegate.getBaseColumn(i, j, levelHeightAccessor, randomState);
    }
    
    @Override
    public void addDebugScreenInfo(List<String> list, RandomState randomState, BlockPos blockPos) {
        delegate.addDebugScreenInfo(list, randomState, blockPos);
    }
}
