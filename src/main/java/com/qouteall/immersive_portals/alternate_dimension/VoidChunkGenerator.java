package com.qouteall.immersive_portals.alternate_dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.Heightmap;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.biome.Biomes;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.FixedBiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.StructuresConfig;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.feature.StructureFeature;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Stream;

public class VoidChunkGenerator extends ChunkGenerator {
    public static Codec<VoidChunkGenerator> codec;
    
    private VerticalBlockSample verticalBlockSample = new VerticalBlockSample(
        Stream.generate(Blocks.AIR::getDefaultState)
            .limit(256)
            .toArray(BlockState[]::new)
    );
    
    public VoidChunkGenerator() {
        super(
            new FixedBiomeSource(Biomes.PLAINS),
            new StructuresConfig(Optional.empty(), new HashMap<>())
        );
    }
    
    @Override
    protected Codec<? extends ChunkGenerator> method_28506() {
        return codec;
    }
    
    @Override
    public ChunkGenerator withSeed(long seed) {
        return this;
    }
    
    @Override
    public void buildSurface(
        ChunkRegion region, Chunk chunk
    ) {
        //nothing
    }
    
    @Override
    public void populateNoise(
        WorldAccess world, StructureAccessor accessor, Chunk chunk
    ) {
        //nothing
    }
    
    @Override
    public void carve(
        long seed, BiomeAccess access, Chunk chunk, GenerationStep.Carver carver
    ) {
        //nothing
    }
    
    @Nullable
    @Override
    public BlockPos locateStructure(
        ServerWorld world,
        StructureFeature<?> feature,
        BlockPos center,
        int radius,
        boolean skipExistingChunks
    ) {
        return null;
    }
    
    @Override
    public void generateFeatures(ChunkRegion region, StructureAccessor accessor) {
        //nothing
    }
    
    @Override
    public void populateEntities(ChunkRegion region) {
        //nothing
    }
    
    @Override
    public void setStructureStarts(
        StructureAccessor structureAccessor, Chunk chunk, StructureManager structureManager, long l
    ) {
        //nothing
    }
    
    @Override
    public void addStructureReferences(
        WorldAccess world, StructureAccessor accessor, Chunk chunk
    ) {
        //nothing
    }
    
    @Override
    public int getHeight(int x, int z, Heightmap.Type heightmapType) {
        return 0;
    }
    
    @Override
    public BlockView getColumnSample(int x, int z) {
        return verticalBlockSample;
    }
    
    static {
        codec = MapCodec.of(
            Encoder.empty(),
            Decoder.unit(VoidChunkGenerator::new)
        ).stable().codec();
    }
}
