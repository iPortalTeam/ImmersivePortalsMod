package com.qouteall.immersive_portals.alternate_dimension;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.qouteall.immersive_portals.Helper;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.structure.StructureManager;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.Identifier;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.Heightmap;
import net.minecraft.world.IWorld;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.gen.ChunkRandom;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.carver.ConfiguredCarver;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.FloatingIslandsChunkGenerator;
import net.minecraft.world.gen.chunk.FloatingIslandsChunkGeneratorConfig;
import net.minecraft.world.gen.feature.*;

import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class ErrorTerrainGenerator extends FloatingIslandsChunkGenerator {
    private final BlockState AIR;
    
    public static final int regionChunkNum = 4;
    public static final int averageY = 64;
    public static final int maxY = 128;
    
    LoadingCache<ChunkPos, RegionErrorTerrainGenerator> cache = CacheBuilder.newBuilder()
        .maximumSize(10000)
        .expireAfterWrite(30, TimeUnit.SECONDS)
        .build(
            new CacheLoader<ChunkPos, RegionErrorTerrainGenerator>() {
                public RegionErrorTerrainGenerator load(ChunkPos key) {
                    return new RegionErrorTerrainGenerator(key.x, key.z, world.getSeed());
                }
            });
    
    public ErrorTerrainGenerator(
        IWorld iWorld,
        BiomeSource biomeSource,
        FloatingIslandsChunkGeneratorConfig floatingIslandsChunkGeneratorConfig
    ) {
        super(iWorld, biomeSource, floatingIslandsChunkGeneratorConfig);
        AIR = Blocks.AIR.getDefaultState();
    }
    
    @Override
    public void populateNoise(IWorld world, Chunk chunk) {
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
    public void carve(BiomeAccess biomeAccess, Chunk chunk, GenerationStep.Carver carver) {
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
                    chunkRandom.setStructureSeed(this.seed + (long) n, cx, cz);
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
    public void generateFeatures(ChunkRegion region) {
        try {
            super.generateFeatures(region);
    
        }
        catch (Throwable throwable) {
            Helper.err("Force ignore exception while generating feature " + throwable);
        }
    
        for (int pass = 0; pass < 2; pass++) {
            int centerChunkX = region.getCenterChunkX();
            int centerChunkZ = region.getCenterChunkZ();
            int x = centerChunkX * 16;
            int z = centerChunkZ * 16;
            BlockPos blockPos = new BlockPos(x, 0, z);
            Biome biome = this.getDecorationBiome(region.getBiomeAccess(), blockPos.add(8, 8, 8));
            ChunkRandom chunkRandom = new ChunkRandom();
            long currSeed = chunkRandom.setSeed(region.getSeed() + pass, x, z);
        
            generateFeatureForStep(
                region, centerChunkX, centerChunkZ,
                blockPos, biome, chunkRandom, currSeed,
                GenerationStep.Feature.UNDERGROUND_ORES
            );
            
        }
    }
    
    private void generateFeatureForStep(
        ChunkRegion region,
        Object centerChunkX,
        Object centerChunkZ,
        BlockPos blockPos,
        Biome biome,
        ChunkRandom chunkRandom,
        long currSeed,
        GenerationStep.Feature feature
    ) {
        try {
            biome.generateFeatureStep(
                feature, this, region, currSeed, chunkRandom, blockPos
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
    
    public void setStructureStarts(
        BiomeAccess biomeAccess,
        Chunk chunk,
        ChunkGenerator<?> chunkGenerator,
        StructureManager structureManager
    ) {
        Iterator var5 = Feature.STRUCTURES.values().iterator();
        
        while (var5.hasNext()) {
            StructureFeature<?> structureFeature = (StructureFeature) var5.next();
            if (chunkGenerator.getBiomeSource().hasStructureFeature(structureFeature)) {
                StructureStart structureStart = chunk.getStructureStart(structureFeature.getName());
                int i = structureStart != null ? structureStart.method_23676() : 0;
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
                        structureFeature,
                        chunkPos.x,
                        chunkPos.z,
                        BlockBox.empty(),
                        i,
                        chunkGenerator.getSeed()
                    );
                    structureStart3.initialize(
                        this,
                        structureManager,
                        chunkPos.x,
                        chunkPos.z,
                        biome
                    );
                    structureStart2 = structureStart3.hasChildren() ? structureStart3 : StructureStart.DEFAULT;
                }
                
                chunk.setStructureStart(structureFeature.getName(), structureStart2);
            }
        }
        
    }
    
    private static double getProbability(StructureFeature<?> structureFeature) {
        if (structureFeature instanceof StrongholdFeature) {
            return 0.02;
        }
        if (structureFeature instanceof MineshaftFeature) {
            return 0.02;
        }
        if (structureFeature instanceof OceanMonumentFeature) {
            return 0.05;
        }
        if (structureFeature instanceof WoodlandMansionFeature) {
            return 0.1;
        }
        if (structureFeature instanceof EndCityFeature) {
            return 0.1;
        }
        return 0.3;
    }
    
    @Override
    public void populateEntities(ChunkRegion region) {
        super.populateEntities(region);
        
        saveVillagers(region);
    }
    
    //make end city and woodland mansion be able to generate
    @Override
    public int getHeightOnGround(int x, int z, Heightmap.Type heightmapType) {
        return 64;
    }
    
    //newly generated villagers usually fall into void or suffocate in wall
    private void saveVillagers(ChunkRegion region) {
        Identifier villagerId = EntityType.getId(EntityType.VILLAGER);
        
        ProtoChunk centerChunk = (ProtoChunk) region.getChunk(
            region.getCenterChunkX(),
            region.getCenterChunkZ()
        );
        
        centerChunk.getEntities().stream()
            .filter(compoundTag ->
                villagerId.toString().equals(compoundTag.getString("id"))
            )
            .forEach(villagerTag -> {
                ListTag posTag = villagerTag.getList("Pos", 6);
                BlockPos villagerBlockPos = new BlockPos(
                    posTag.getDouble(0),
                    posTag.getDouble(1),
                    posTag.getDouble(2)
                );
                
                IntStream.range(-32, 64)
                    .mapToObj(yOffset -> villagerBlockPos.add(0, yOffset, 0))
                    .filter(
                        pos -> region.getBlockState(pos).isAir() &&
                            region.getBlockState(pos.add(0, 1, 0)).isAir() &&
                            !region.getBlockState(pos.add(0, -1, 0)).isAir()
                    )
                    .findFirst()
                    .ifPresent(pos -> {
                        posTag.set(0, DoubleTag.of(pos.getX()));
                        posTag.set(1, DoubleTag.of(pos.getY()));
                        posTag.set(2, DoubleTag.of(pos.getZ()));
                    });
                
                
            });
    }
}
