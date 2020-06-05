package com.qouteall.immersive_portals.alternate_dimension;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.qouteall.immersive_portals.Helper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.class_5284;
import net.minecraft.structure.StructureManager;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.Heightmap;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.gen.ChunkRandom;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.carver.ConfiguredCarver;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.FloatingIslandsChunkGenerator;
import net.minecraft.world.gen.feature.EndCityFeature;
import net.minecraft.world.gen.feature.Feature;
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

public class ErrorTerrainGenerator extends FloatingIslandsChunkGenerator {
    private final BlockState AIR;
    
    public static final int regionChunkNum = 4;
    public static final int averageY = 64;
    public static final int maxY = 128;
    
    LoadingCache<ChunkPos, RegionErrorTerrainGenerator> cache;
    
    public ErrorTerrainGenerator(
        BiomeSource biomeSource,
        long seed,
        class_5284 config
    ) {
        super(biomeSource, seed, config);
        AIR = Blocks.AIR.getDefaultState();
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
    public void populateNoise(WorldAccess world, StructureAccessor structureAccessor, Chunk chunk) {
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
    
    //carve more
    @Override
    public void carve(long seed,BiomeAccess biomeAccess, Chunk chunk, GenerationStep.Carver carver) {
        ChunkRandom chunkRandom = new ChunkRandom();
        ChunkPos chunkPos = chunk.getPos();
        int chunkX = chunkPos.x;
        int chunkZ = chunkPos.z;
        Biome biome = this.getDecorationBiome(biomeAccess, chunkPos.getCenterBlockPos());
        BitSet bitSet = chunk.getCarvingMask(carver);
        
        for (int cx = chunkX - 8; cx <= chunkX + 8; ++cx) {
            for (int cz = chunkZ - 8; cz <= chunkZ + 8; ++cz) {
                List<ConfiguredCarver<?>> list = biome.getCarversForStep(carver);
                ListIterator listIterator = list.listIterator();
                
                while (listIterator.hasNext()) {
                    int n = listIterator.nextIndex();
                    ConfiguredCarver<?> configuredCarver = (ConfiguredCarver) listIterator.next();
                    chunkRandom.setCarverSeed(seed + (long) n, cx, cz);
                    boolean shouldCarve = configuredCarver.shouldCarve(chunkRandom, cx, cz);
                    if (shouldCarve) {
                        //carve more
                        for (int i = 0; i < 4; i++) {
                            configuredCarver.carve(chunk, (blockPos) -> {
                                return this.getDecorationBiome(biomeAccess, blockPos);
                            }, chunkRandom, this.getSeaLevel(), cx, cz, chunkX, chunkZ, bitSet);
                        }
                    }
                }
            }
        }
        
    }
    
    //generate more ore
    @Override
    public void generateFeatures(ChunkRegion region, StructureAccessor structureAccessor) {
        try {
            super.generateFeatures(region, structureAccessor);
            
        }
        catch (Throwable throwable) {
            Helper.err("Force ignore exception while generating feature " + throwable);
        }
        
        int centerChunkX = region.getCenterChunkX();
        int centerChunkZ = region.getCenterChunkZ();
        int x = centerChunkX * 16;
        int z = centerChunkZ * 16;
        BlockPos blockPos = new BlockPos(x, 0, z);
        
        for (int pass = 1; pass < 4; pass++) {
            Biome biome = this.getDecorationBiome(region.getBiomeAccess(), blockPos.add(8, 8, 8));
            ChunkRandom chunkRandom = new ChunkRandom();
            long currSeed = chunkRandom.setPopulationSeed(region.getSeed() + pass, x, z);
            
            generateFeatureForStep(
                region, centerChunkX, centerChunkZ,
                blockPos, biome, chunkRandom, currSeed,
                GenerationStep.Feature.UNDERGROUND_ORES,
                structureAccessor
            );
        }
        
        SpongeDungeonFeature.instance.generate(
            region,
            structureAccessor,
            this,
            random,
            blockPos,
            null
        );
    }
    
    private void generateFeatureForStep(
        ChunkRegion region,
        Object centerChunkX,
        Object centerChunkZ,
        BlockPos blockPos,
        Biome biome,
        ChunkRandom chunkRandom,
        long currSeed,
        GenerationStep.Feature feature,
        StructureAccessor structureAccessor
    ) {
        try {
            biome.generateFeatureStep(
                feature, structureAccessor, this, region, currSeed, chunkRandom, blockPos
            );
        }
        catch (Exception var17) {
            CrashReport crashReport = CrashReport.create(var17, "Biome decoration");
            crashReport.addElement("Generation").add("CenterX", centerChunkX).add(
                "CenterZ",
                centerChunkZ
            ).add("Step", (Object) feature).add("Seed", (Object) currSeed).add(
                "Biome",
                (Object) Registry.BIOME.getId(biome)
            );
            throw new CrashException(crashReport);
        }
    }
    
    @Override
    public void setStructureStarts(
        StructureAccessor accessor,
        BiomeAccess biomeAccess,
        Chunk chunk,
        ChunkGenerator generator,
        StructureManager manager,
        long seed
    ) {
        random.setTerrainSeed(chunk.getPos().x, chunk.getPos().z);
        
        Iterator var5 = Feature.STRUCTURES.values().iterator();
        
        while (var5.hasNext()) {
            StructureFeature<?> structureFeature = (StructureFeature) var5.next();
            if (generator.getBiomeSource().hasStructureFeature(structureFeature)) {
                StructureStart structureStart = chunk.getStructureStart(structureFeature.getName());
                int i = structureStart != null ? structureStart.getReferences() : 0;
                ChunkRandom chunkRandom = new ChunkRandom();
                ChunkPos chunkPos = chunk.getPos();
                StructureStart structureStart2 = StructureStart.DEFAULT;
                Biome biome = biomeAccess.getBiome(new BlockPos(
                    chunkPos.getStartX() + 9,
                    0,
                    chunkPos.getStartZ() + 9
                ));
                boolean shouldStart = hasStructure(biome, structureFeature);
                if (random.nextDouble() > getProbability(structureFeature)) {
                    shouldStart = false;
                }
                if (shouldStart) {
                    StructureStart structureStart3 = structureFeature.getStructureStartFactory().create(
                        structureFeature, chunkPos.x, chunkPos.z, BlockBox.empty(), i, seed
                    );
                    structureStart3.init(
                        this, manager, chunkPos.x, chunkPos.z, biome
                    );
                    structureStart2 = structureStart3.hasChildren() ? structureStart3 : StructureStart.DEFAULT;
                }
                
                chunk.setStructureStart(structureFeature.getName(), structureStart2);
            }
        }
        
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
        super.populateEntities(region);
        
    }
    
    @Override
    public void buildSurface(ChunkRegion chunkRegion, Chunk chunk) {
        super.buildSurface(chunkRegion, chunk);
        avoidSandLag(chunkRegion);
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
    
    //make end city and woodland mansion be able to generate
    @Override
    public int getHeightOnGround(int x, int z, Heightmap.Type heightmapType) {
        return 64;
    }
    
    @Override
    public void addStructureReferences(
        WorldAccess world,
        StructureAccessor structureAccessor,
        Chunk chunk
    ) {
        try {
            super.addStructureReferences(world, structureAccessor, chunk);
        }
        catch (Throwable throwable) {
            Helper.err("Error Generating " + world.getDimension().getType() + chunk.getPos());
            throwable.printStackTrace();
        }
    }
}
