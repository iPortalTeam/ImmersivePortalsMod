package com.qouteall.immersive_portals.alternate_dimension;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.ducks.IESurfaceChunkGenerator;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.IWorld;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.SeedMixer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.gen.chunk.FloatingIslandsChunkGenerator;
import net.minecraft.world.gen.chunk.FloatingIslandsChunkGeneratorConfig;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class MyKaleidoscopeGenerator extends FloatingIslandsChunkGenerator {
    private final BlockState AIR;
    
    private static final int regionChunkNum = 3;
    private static final int averageY = 64;
    private static final int maxY = 128;
    
    LoadingCache<ChunkPos, RegionGenerationInfo> cache = CacheBuilder.newBuilder()
        .maximumSize(10000)
        .expireAfterWrite(30, TimeUnit.SECONDS)
        .build(
            new CacheLoader<ChunkPos, RegionGenerationInfo>() {
                public RegionGenerationInfo load(ChunkPos key) {
                    return new RegionGenerationInfo(key.x, key.z, world.getSeed());
                }
            });
    
    private static class RegionGenerationInfo {
        public int regionX;
        public int regionZ;
        public FormulaGenerator.TriNumFunction expression;
        public double singleAverage;
        //public double[] average;
        
        public RegionGenerationInfo(
            int regionX_,
            int regionZ_,
            long seed
        ) {
            regionX = regionX_;
            regionZ = regionZ_;
            
            initExpression(seed);
            
            calculateAverage();
        }
        
        private void initExpression(long seed) {
            long realSeed = SeedMixer.mixSeed(
                seed, SeedMixer.mixSeed(
                    regionX, regionZ
                )
            );
            Random random = new Random(realSeed);
            expression = FormulaGenerator.getRandomTriCompositeExpression(random);
        }
        
        private void calculateAverage() {
            int a = regionChunkNum * 16;
            
            final int sampleBlock = 4;
            
            int horizontalSampleNum = a / sampleBlock + 1;
            int verticalSampleNum = maxY / sampleBlock + 1;
            
            double sum = 0;
            for (int x = 0; x < horizontalSampleNum; x++) {
                for (int z = 0; z < horizontalSampleNum; z++) {
                    for (int y = 0; y < verticalSampleNum; y++) {
                        sum += expression.eval(
                            x * sampleBlock,
                            y * sampleBlock,
                            z * sampleBlock
                        );
                    }
                }
            }
            
            singleAverage = sum / (horizontalSampleNum * horizontalSampleNum * verticalSampleNum);

//            average = new double[maxY];
//
//            for (int y = 0; y < maxY; y++) {
//                double sum = expression.eval(0, y, 0) +
//                    expression.eval(0, y, a) +
//                    expression.eval(a, y, 0) +
//                    expression.eval(a, y, a) +
//                    expression.eval(a / 2, y, a / 2)*2;
//                average[y] = sum / 6;
//            }
        }
        
        public double calc(int worldX, int worldY, int worldZ) {
            int regionStartX = regionX * 16 * regionChunkNum;
            int regionStartZ = regionZ * 16 * regionChunkNum;
            return expression.eval(
                worldX - regionStartX,
                worldY,
                worldZ - regionStartZ
            );
        }
    }
    
    public MyKaleidoscopeGenerator(
        IWorld iWorld,
        BiomeSource biomeSource,
        FloatingIslandsChunkGeneratorConfig floatingIslandsChunkGeneratorConfig
    ) {
        super(iWorld, biomeSource, floatingIslandsChunkGeneratorConfig);
        AIR = Blocks.AIR.getDefaultState();
    }
    
    
    @Override
    public void populateNoise(IWorld world, Chunk chunk) {
        
        IESurfaceChunkGenerator ie = (IESurfaceChunkGenerator) this;
        
        int verticalNoiseResolution_ = ie.get_verticalNoiseResolution();
        int horizontalNoiseResolution_ = ie.get_horizontalNoiseResolution();
        int noiseSizeX_ = ie.get_noiseSizeX();
        int noiseSizeY_ = ie.get_noiseSizeY();
        int noiseSizeZ_ = ie.get_noiseSizeZ();
        
        int seaLevel = getSeaLevel();
        
        ProtoChunk protoChunk = (ProtoChunk) chunk;
        ChunkPos pos = chunk.getPos();
        Heightmap oceanFloorHeightMap = protoChunk.getHeightmap(Heightmap.Type.OCEAN_FLOOR_WG);
        Heightmap surfaceHeightMap = protoChunk.getHeightmap(Heightmap.Type.WORLD_SURFACE_WG);
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        
        int regionX = Math.floorDiv(pos.x, regionChunkNum);
        int regionZ = Math.floorDiv(pos.z, regionChunkNum);
//        RegionGenerationInfo info = new RegionGenerationInfo(
//            regionX,
//            regionZ,
//            world.getSeed()
//        );
        RegionGenerationInfo info = Helper.noError(() ->
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
                        
                        BlockComposition composition =
                            getBlockComposition(info, worldX, worldY, worldZ);
                        
                        BlockState currBlockState;
                        
                        switch (composition) {
                            case air:
                                currBlockState = AIR;
                                break;
                            case stone:
                                currBlockState = defaultBlock;
                                break;
                            default:
                            case water:
                                currBlockState = defaultFluid;
                                break;
                        }
                        
                        if (currBlockState != AIR) {
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
    
    enum BlockComposition {
        stone,
        water,
        air
    }
    
    
    private BlockComposition getBlockComposition(
        RegionGenerationInfo info,
        int worldX, int worldY, int worldZ
    ) {
        
        if (worldY >= maxY) {
            return BlockComposition.air;
        }
        
        double value = info.calc(worldX, worldY, worldZ);
        
        double valve = info.singleAverage;
        
        valve *= Math.exp(Math.abs(worldY - averageY) / 32.0);
        
        if (value > valve) {
            return BlockComposition.stone;
        }
        else {
            return BlockComposition.air;
        }
    }
    
}
