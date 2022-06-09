package qouteall.imm_ptl.peripheral.alternate_dimension;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.jetbrains.annotations.Nullable;
import qouteall.imm_ptl.peripheral.mixin.common.alternate_dimension.IEChunk1;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Stream;

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
    
    @Override
    public CompletableFuture<ChunkAccess> createBiomes(Registry<Biome> registry, Executor executor, RandomState randomState, Blender blender, StructureManager structureManager, ChunkAccess chunkAccess) {
        return delegate.createBiomes(registry, executor, randomState, blender, structureManager, chunkAccess);
    }
    
    @Override
    public void applyBiomeDecoration(WorldGenLevel worldGenLevel, ChunkAccess chunkAccess, StructureManager structureManager) {
        delegate.applyBiomeDecoration(worldGenLevel, chunkAccess, structureManager);
    }
    
    @Override
    public Stream<Holder<StructureSet>> possibleStructureSets() {
        return delegate.possibleStructureSets();
    }
    
    @Override
    public Optional<ResourceKey<Codec<? extends ChunkGenerator>>> getTypeNameForDataFixer() {
        return delegate.getTypeNameForDataFixer();
    }
    
    @Nullable
    @Override
    public Pair<BlockPos, Holder<Structure>> findNearestMapStructure(ServerLevel serverLevel, HolderSet<Structure> holderSet, BlockPos blockPos, int i, boolean bl) {
        return delegate.findNearestMapStructure(serverLevel, holderSet, blockPos, i, bl);
    }
    
    @Override
    public boolean hasStructureChunkInRange(Holder<StructureSet> holder, RandomState randomState, long l, int i, int j, int k) {
        return delegate.hasStructureChunkInRange(holder, randomState, l, i, j, k);
    }
    
    @Override
    public int getSpawnHeight(LevelHeightAccessor level) {
        return delegate.getSpawnHeight(level);
    }
    
    @Override
    public BiomeSource getBiomeSource() {
        return delegate.getBiomeSource();
    }
    
    @Override
    public WeightedRandomList<MobSpawnSettings.SpawnerData> getMobsAt(Holder<Biome> holder, StructureManager structureManager, MobCategory mobCategory, BlockPos blockPos) {
        return delegate.getMobsAt(holder, structureManager, mobCategory, blockPos);
    }
    
    @Override
    public void createStructures(RegistryAccess registryAccess, RandomState randomState, StructureManager structureManager, ChunkAccess chunkAccess, StructureTemplateManager structureTemplateManager, long l) {
        delegate.createStructures(registryAccess, randomState, structureManager, chunkAccess, structureTemplateManager, l);
    }
    
    @Override
    public void createReferences(WorldGenLevel worldGenLevel, StructureManager structureManager, ChunkAccess chunkAccess) {
        delegate.createReferences(worldGenLevel, structureManager, chunkAccess);
    }
    
    @Override
    public int getFirstFreeHeight(int i, int j, Heightmap.Types types, LevelHeightAccessor levelHeightAccessor, RandomState randomState) {
        return delegate.getFirstFreeHeight(i, j, types, levelHeightAccessor, randomState);
    }
    
    @Override
    public int getFirstOccupiedHeight(int i, int j, Heightmap.Types types, LevelHeightAccessor levelHeightAccessor, RandomState randomState) {
        return delegate.getFirstOccupiedHeight(i, j, types, levelHeightAccessor, randomState);
    }
    
    @Override
    public void ensureStructuresGenerated(RandomState randomState) {
        delegate.ensureStructuresGenerated(randomState);
    }
    
    @Nullable
    @Override
    public List<ChunkPos> getRingPositionsFor(ConcentricRingsStructurePlacement concentricRingsStructurePlacement, RandomState randomState) {
        return delegate.getRingPositionsFor(concentricRingsStructurePlacement, randomState);
    }
    
    @Override
    public BiomeGenerationSettings getBiomeGenerationSettings(Holder<Biome> holder) {
        return delegate.getBiomeGenerationSettings(holder);
    }
}
