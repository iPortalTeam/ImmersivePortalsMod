package com.qouteall.immersive_portals.alternate_dimension;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.qouteall.immersive_portals.Helper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.structure.StructureManager;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.noise.OctaveSimplexNoiseSampler;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.BlockView;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.Heightmap;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.gen.ChunkRandom;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.carver.ConfiguredCarver;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.StructureConfig;
import net.minecraft.world.gen.chunk.StructuresConfig;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.feature.ConfiguredStructureFeature;
import net.minecraft.world.gen.feature.EndCityFeature;
import net.minecraft.world.gen.feature.MineshaftFeature;
import net.minecraft.world.gen.feature.OceanMonumentFeature;
import net.minecraft.world.gen.feature.StrongholdFeature;
import net.minecraft.world.gen.feature.StructureFeature;
import net.minecraft.world.gen.feature.WoodlandMansionFeature;

import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ErrorTerrainGenerator extends ChunkGenerator {
    public static final Codec<ErrorTerrainGenerator> codec = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.LONG.fieldOf("seed").stable().forGetter(g -> g.worldSeed)
        ).apply(instance, instance.stable(ErrorTerrainGenerator::new))
    );
    
    private final BlockState air = Blocks.AIR.getDefaultState();
    private final BlockState defaultBlock = Blocks.STONE.getDefaultState();
    private final BlockState defaultFluid = Blocks.WATER.getDefaultState();
    
    
    public static final int regionChunkNum = 4;
    public static final int averageY = 64;
    public static final int maxY = 128;
    
    private static final VerticalBlockSample verticalBlockSample = new VerticalBlockSample(
        Stream.concat(
            Stream.generate(Blocks.STONE::getDefaultState).limit(64),
            Stream.generate(Blocks.AIR::getDefaultState).limit(128 + 64)
        ).toArray(BlockState[]::new)
    );
    
    private long worldSeed;
    
    private final LoadingCache<ChunkPos, RegionErrorTerrainGenerator> cache = CacheBuilder.newBuilder()
        .maximumSize(10000)
        .expireAfterWrite(30, TimeUnit.SECONDS)
        .build(
            new CacheLoader<ChunkPos, RegionErrorTerrainGenerator>() {
                public RegionErrorTerrainGenerator load(ChunkPos key) {
                    return new RegionErrorTerrainGenerator(key.x, key.z, worldSeed);
                }
            });
    
    private final OctaveSimplexNoiseSampler surfaceDepthNoise;
    
    public ErrorTerrainGenerator(long seed) {
        super(new ChaosBiomeSource(seed), new StructuresConfig(true));
        worldSeed = seed;
        
        surfaceDepthNoise = new OctaveSimplexNoiseSampler(new ChunkRandom(
            seed), IntStream.rangeClosed(-3, 0));
    }
    
    private static double getProbability(StructureFeature<?> structureFeature) {
        if (structureFeature instanceof StrongholdFeature) {
            return 0.0007;
        }
        if (structureFeature instanceof MineshaftFeature) {
            return 0.015;
        }
        if (structureFeature instanceof OceanMonumentFeature) {
            return 0.03;
        }
        if (structureFeature instanceof WoodlandMansionFeature) {
            return 0.08;
        }
        if (structureFeature instanceof EndCityFeature) {
            return 0.2;
        }
        return 0.15;
    }
    
    @Override
    public void populateEntities(ChunkRegion region) {
        int i = region.getCenterChunkX();
        int j = region.getCenterChunkZ();
        Biome biome = region.getBiome((new ChunkPos(i, j)).getCenterBlockPos());
        ChunkRandom chunkRandom = new ChunkRandom();
        chunkRandom.setPopulationSeed(region.getSeed(), i << 4, j << 4);
        SpawnHelper.populateEntities(region, biome, i, j, chunkRandom);
    }
    
    @Override
    public void populateNoise(
        WorldAccess world, StructureAccessor accessor, Chunk chunk
    ) {
        ProtoChunk protoChunk = (ProtoChunk) chunk;
        ChunkPos pos = chunk.getPos();
        Heightmap oceanFloorHeightMap = protoChunk.getHeightmap(Heightmap.Type.OCEAN_FLOOR_WG);
        Heightmap surfaceHeightMap = protoChunk.getHeightmap(Heightmap.Type.WORLD_SURFACE_WG);
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        
        int regionX = Math.floorDiv(pos.x, regionChunkNum);
        int regionZ = Math.floorDiv(pos.z, regionChunkNum);
        RegionErrorTerrainGenerator generator = Helper.noError(() ->
            cache.get(new ChunkPos(regionX, regionZ))
        );
        
        for (int sectionY = 0; sectionY < 16; sectionY++) {
            ChunkSection section = protoChunk.getSection(sectionY);
            section.lock();
            
            for (int localX = 0; localX < 16; localX++) {
                for (int localY = 0; localY < 16; localY++) {
                    for (int localZ = 0; localZ < 16; localZ++) {
                        int worldX = pos.x * 16 + localX;
                        int worldY = sectionY * 16 + localY;
                        int worldZ = pos.z * 16 + localZ;
                        
                        BlockState currBlockState = generator.getBlockComposition(
                            worldX, worldY, worldZ
                        );
                        
                        if (currBlockState != air) {
                            section.setBlockState(localX, localY, localZ, currBlockState, false);
                            oceanFloorHeightMap.trackUpdate(localX, worldY, localZ, currBlockState);
                            surfaceHeightMap.trackUpdate(localX, worldY, localZ, currBlockState);
                        }
                    }
                }
            }
            
            section.unlock();
        }
        
    }
    
    @Override
    public int getHeight(int x, int z, Heightmap.Type heightmapType) {
        return 64;
    }
    
    //may be incorrect
    @Override
    public BlockView getColumnSample(int x, int z) {
        return verticalBlockSample;
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
    public void buildSurface(ChunkRegion region, Chunk chunk) {
        ChunkPos chunkPos = chunk.getPos();
        int i = chunkPos.x;
        int j = chunkPos.z;
        ChunkRandom chunkRandom = new ChunkRandom();
        chunkRandom.setTerrainSeed(i, j);
        ChunkPos chunkPos2 = chunk.getPos();
        int k = chunkPos2.getStartX();
        int l = chunkPos2.getStartZ();
        double d = 0.0625D;
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        
        for (int m = 0; m < 16; ++m) {
            for (int n = 0; n < 16; ++n) {
                int o = k + m;
                int p = l + n;
                int q = chunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE_WG, m, n) + 1;
                double e = this.surfaceDepthNoise.sample(
                    (double) o * 0.0625D,
                    (double) p * 0.0625D,
                    0.0625D,
                    (double) m * 0.0625D
                ) * 15.0D;
                region.getBiome(mutable.set(k + m, q, l + n)).buildSurface(
                    chunkRandom,
                    chunk,
                    o,
                    p,
                    q,
                    e,
                    this.defaultBlock,
                    this.defaultFluid,
                    this.getSeaLevel(),
                    region.getSeed()
                );
            }
        }
        
        
        avoidSandLag(region);
    }
    
    //carve more
    @Override
    public void carve(
        long seed, BiomeAccess access, Chunk chunk, GenerationStep.Carver carver
    ) {
        BiomeAccess biomeAccess = access.withSource(this.biomeSource);
        ChunkRandom chunkRandom = new ChunkRandom();
        ChunkPos chunkPos = chunk.getPos();
        Biome biome = this.biomeSource.getBiomeForNoiseGen(
            chunkPos.x << 2, 0, chunkPos.z << 2
        );
        BitSet bitSet = ((ProtoChunk) chunk).getOrCreateCarvingMask(carver);
        
        for (int num = 0; num < 4; num++) {
            for (int cx = chunkPos.x - 8; cx <= chunkPos.x + 8; ++cx) {
                for (int cz = chunkPos.z - 8; cz <= chunkPos.z + 8; ++cz) {
                    List<ConfiguredCarver<?>> list = biome.getCarversForStep(carver);
                    ListIterator listIterator = list.listIterator();
                    
                    while (listIterator.hasNext()) {
                        int n = listIterator.nextIndex();
                        ConfiguredCarver<?> configuredCarver = (ConfiguredCarver) listIterator.next();
                        chunkRandom.setCarverSeed(seed + (long) n + num * 2333, cx, cz);
                        if (configuredCarver.shouldCarve(chunkRandom, cx, cz)) {
                            configuredCarver.carve(
                                chunk,
                                biomeAccess::getBiome,
                                chunkRandom,
                                this.getSeaLevel(),
                                cx,
                                cz,
                                chunkPos.x,
                                chunkPos.z,
                                bitSet
                            );
                        }
                    }
                }
            }
        }
        
    }
    
    private static void avoidSandLag(ChunkRegion region) {
        Chunk centerChunk = region.getChunk(region.getCenterChunkX(), region.getCenterChunkZ());
        BlockPos.Mutable temp = new BlockPos.Mutable();
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                boolean isLastAir = true;
                for (int y = 0; y < 100; y++) {
                    temp.set(x, y, z);
                    BlockState blockState = centerChunk.getBlockState(temp);
                    Block block = blockState.getBlock();
                    if (block == Blocks.SAND || block == Blocks.GRAVEL) {
                        if (isLastAir) {
                            centerChunk.setBlockState(
                                temp,
                                Blocks.SANDSTONE.getDefaultState(),
                                true
                            );
                        }
                    }
                    isLastAir = blockState.isAir();
                }
            }
        }
    }
}
