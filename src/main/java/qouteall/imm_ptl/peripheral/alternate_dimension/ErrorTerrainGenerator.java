package qouteall.imm_ptl.peripheral.alternate_dimension;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
import net.minecraft.world.gen.chunk.StructuresConfig;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import qouteall.q_misc_util.Helper;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public class ErrorTerrainGenerator extends DelegatedChunkGenerator {
    
    
    public static final int regionChunkNum = 4;
    public static final int averageY = 64;
    public static final int maxY = 128;
    
    private final BlockState air = Blocks.AIR.getDefaultState();
    private final BlockState defaultBlock = Blocks.STONE.getDefaultState();
    private final BlockState defaultFluid = Blocks.WATER.getDefaultState();
    
    private final LoadingCache<ChunkPos, RegionErrorTerrainGenerator> cache;
    
    public ErrorTerrainGenerator(long seed, ChunkGenerator delegate, BiomeSource biomeSource) {
        super(biomeSource, new StructuresConfig(true), delegate);
        
        cache = CacheBuilder.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(30, TimeUnit.SECONDS)
            .build(
                new CacheLoader<ChunkPos, RegionErrorTerrainGenerator>() {
                    public RegionErrorTerrainGenerator load(ChunkPos key) {
                        return new RegionErrorTerrainGenerator(key.x, key.z, seed);
                    }
                });
    }
    
    @Override
    public ChunkGenerator withSeed(long seed) {
        return new ErrorTerrainGenerator(seed, delegate.withSeed(seed), biomeSource_);
    }
    
    @Override
    public CompletableFuture<Chunk> populateNoise(Executor executor, Blender arg, StructureAccessor structureAccessor, Chunk chunk) {
        ChunkSection[] sectionArray = chunk.getSectionArray();
        ArrayList<ChunkSection> locked = new ArrayList<>();
        for (ChunkSection chunkSection : sectionArray) {
            if (chunkSection != null) {
                chunkSection.lock();
                locked.add(chunkSection);
            }
        }
        return CompletableFuture.supplyAsync(() -> {
            doPopulateNoise(chunk);
            return chunk;
        }, executor).thenApplyAsync((chunkx) -> {
            for (ChunkSection chunkSection : locked) {
                chunkSection.unlock();
            }
            
            return chunkx;
        }, executor);
    }
    
    public void doPopulateNoise(Chunk chunk) {
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
//            section.lock();
            
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

//            section.unlock();
        }
        
    }
    
}
