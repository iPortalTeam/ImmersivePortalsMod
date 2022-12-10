package qouteall.imm_ptl.peripheral.alternate_dimension;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.jetbrains.annotations.NotNull;
import qouteall.imm_ptl.peripheral.mixin.common.alternate_dimension.IENoiseGeneratorSettings;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.MiscHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ErrorTerrainGenerator extends DelegatedChunkGenerator {
    
    public static final Codec<ErrorTerrainGenerator> codec = RecordCodecBuilder.create(
        instance -> instance.group(
                RegistryOps.retrieveGetter(Registries.BIOME),
                RegistryOps.retrieveGetter(Registries.NOISE_SETTINGS)
            )
            .apply(instance, ErrorTerrainGenerator::create)
    );
    
    public static ErrorTerrainGenerator create(
        HolderGetter<Biome> biomeRegistry,
        HolderGetter<NoiseGeneratorSettings> noiseGeneratorSettingsHolderGetter
    ) {
        ResourceKey<Biome>[] biomes = new ResourceKey[]{
            Biomes.DESERT
        };
        
        List<Holder.Reference<Biome>> holders = Arrays.stream(biomes)
            .map(biomeRegistry::getOrThrow)
            .collect(Collectors.toList());
        
        ChaosBiomeSource chaosBiomeSource = new ChaosBiomeSource(
            HolderSet.direct(holders)
        );
        
        NoiseGeneratorSettings skylandSetting = noiseGeneratorSettingsHolderGetter
            .getOrThrow(NoiseGeneratorSettings.FLOATING_ISLANDS).value();
        
        NoiseBasedChunkGenerator islandChunkGenerator = new NoiseBasedChunkGenerator(
            chaosBiomeSource, Holder.direct(skylandSetting)
        );
        
        return new ErrorTerrainGenerator(
            chaosBiomeSource, islandChunkGenerator
        );
    }
    
    public static final int regionChunkNum = 4;
    public static final int averageY = 64;
    public static final int maxY = 128;
    
    private final BlockState air = Blocks.AIR.defaultBlockState();
    private final BlockState defaultBlock = Blocks.STONE.defaultBlockState();
    private final BlockState defaultFluid = Blocks.WATER.defaultBlockState();
    
    private final LoadingCache<ChunkPos, RegionErrorTerrainGenerator> cache;
    
    
    public ErrorTerrainGenerator(
        BiomeSource biomeSource, ChunkGenerator delegate
    ) {
        super(biomeSource, delegate);
        
        cache = CacheBuilder.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(30, TimeUnit.SECONDS)
            .build(
                new CacheLoader<ChunkPos, RegionErrorTerrainGenerator>() {
                    public RegionErrorTerrainGenerator load(ChunkPos key) {
                        return new RegionErrorTerrainGenerator(
                            key.x, key.z,
                            System.nanoTime()
                            // use the system time as seed
                            // there is no need to keep the error terrain generation consistent
                        );
                    }
                });
    }
    
    @Override
    protected Codec<? extends ChunkGenerator> codec() {
        return codec;
    }
    
    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender, RandomState randomState, StructureManager structureManager, ChunkAccess chunkAccess) {
        LevelChunkSection[] sectionArray = chunkAccess.getSections();
        ArrayList<LevelChunkSection> locked = new ArrayList<>();
        for (LevelChunkSection chunkSection : sectionArray) {
            if (chunkSection != null) {
                chunkSection.acquire();
                locked.add(chunkSection);
            }
        }
        return CompletableFuture.supplyAsync(() -> {
            doPopulateNoise(chunkAccess);
            return chunkAccess;
        }, executor).thenApplyAsync((chunkx) -> {
            for (LevelChunkSection chunkSection : locked) {
                chunkSection.release();
            }
            
            return chunkx;
        }, executor);
    }
    
    public void doPopulateNoise(ChunkAccess chunk) {
        ProtoChunk protoChunk = (ProtoChunk) chunk;
        ChunkPos pos = chunk.getPos();
        Heightmap oceanFloorHeightMap = protoChunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap surfaceHeightMap = protoChunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        
        int regionX = Math.floorDiv(pos.x, regionChunkNum);
        int regionZ = Math.floorDiv(pos.z, regionChunkNum);
        RegionErrorTerrainGenerator generator = Helper.noError(() ->
            cache.get(new ChunkPos(regionX, regionZ))
        );
        
        for (int sectionY = 0; sectionY < 16; sectionY++) {
            LevelChunkSection section = protoChunk.getSection(sectionY);
            
            for (int localX = 0; localX < 16; localX++) {
                for (int localZ = 0; localZ < 16; localZ++) {
                    for (int localY = 0; localY < 16; localY++) {
                        int worldX = pos.x * 16 + localX;
                        int worldY = sectionY * 16 + localY;
                        int worldZ = pos.z * 16 + localZ;
                        
                        BlockState currBlockState = generator.getBlockComposition(
                            worldX, worldY, worldZ
                        );
                        
                        if (currBlockState != air) {
                            section.setBlockState(localX, localY, localZ, currBlockState, false);
                            oceanFloorHeightMap.update(localX, worldY, localZ, currBlockState);
                            surfaceHeightMap.update(localX, worldY, localZ, currBlockState);
                        }
                    }
                }
            }
        }
    }
    
}
