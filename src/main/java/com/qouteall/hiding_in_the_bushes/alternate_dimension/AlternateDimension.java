package com.qouteall.hiding_in_the_bushes.alternate_dimension;

import com.qouteall.immersive_portals.alternate_dimension.ChaosBiomeSource;
import com.qouteall.immersive_portals.alternate_dimension.ErrorTerrainGenerator;
import com.qouteall.immersive_portals.alternate_dimension.NormalSkylandGenerator;
import com.qouteall.immersive_portals.alternate_dimension.StretchedSkylandGenerator;
import com.qouteall.immersive_portals.alternate_dimension.VoidChunkGenerator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biomes;
import net.minecraft.world.biome.source.BiomeSourceType;
import net.minecraft.world.biome.source.VanillaLayeredBiomeSource;
import net.minecraft.world.biome.source.VanillaLayeredBiomeSourceConfig;
import net.minecraft.world.dimension.Dimension;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.ChunkGeneratorType;
import net.minecraft.world.gen.chunk.FloatingIslandsChunkGeneratorConfig;
import net.minecraft.world.gen.chunk.OverworldChunkGeneratorConfig;

import java.util.Random;
import java.util.function.Function;
import java.util.function.Supplier;

public class AlternateDimension extends Dimension {
    
    public static final BlockPos SPAWN = new BlockPos(100, 50, 0);
    
    private static Random random = new Random();
    Function<AlternateDimension, ChunkGenerator> chunkGeneratorFunction;
    Supplier<DimensionType> dimensionTypeSupplier;
    
    public AlternateDimension(
        World worldIn,
        DimensionType typeIn,
        Function<AlternateDimension, ChunkGenerator> chunkGeneratorFunction_,
        Supplier<DimensionType> dimensionTypeSupplier_
    ) {
        super(worldIn, typeIn, 0);
        chunkGeneratorFunction = chunkGeneratorFunction_;
        dimensionTypeSupplier = dimensionTypeSupplier_;
    }
    
    public ChunkGenerator<?> createChunkGenerator() {
        return chunkGeneratorFunction.apply(this);
    }
    
    public ChunkGenerator<?> getChunkGenerator2() {
        OverworldChunkGeneratorConfig overworldChunkGeneratorConfig2 =
            (OverworldChunkGeneratorConfig) ChunkGeneratorType.SURFACE.createSettings();
        VanillaLayeredBiomeSourceConfig vanillaLayeredBiomeSourceConfig2 =
            ((VanillaLayeredBiomeSourceConfig) BiomeSourceType.VANILLA_LAYERED.getConfig(this.world.getLevelProperties())).setGeneratorSettings(
                overworldChunkGeneratorConfig2);
    
        VanillaLayeredBiomeSource newBiomeSource =
            BiomeSourceType.VANILLA_LAYERED.applyConfig(vanillaLayeredBiomeSourceConfig2);
    
        FloatingIslandsChunkGeneratorConfig generationSettings = ChunkGeneratorType.FLOATING_ISLANDS.createSettings();
        generationSettings.setDefaultBlock(Blocks.STONE.getDefaultState());
        generationSettings.setDefaultFluid(Blocks.AIR.getDefaultState());
        generationSettings.withCenter(this.getForcedSpawnPoint());
        return new NormalSkylandGenerator(
            this.world,
            newBiomeSource,
            generationSettings
        );
    }
    
    public ChunkGenerator<?> getChunkGenerator1() {
        FloatingIslandsChunkGeneratorConfig generationSettings = ChunkGeneratorType.FLOATING_ISLANDS.createSettings();
        generationSettings.setDefaultBlock(Blocks.STONE.getDefaultState());
        generationSettings.setDefaultFluid(Blocks.WATER.getDefaultState());
        generationSettings.withCenter(this.getForcedSpawnPoint());
        return ChunkGeneratorType.FLOATING_ISLANDS.create(
            this.world,
            BiomeSourceType.FIXED.applyConfig(
                BiomeSourceType.FIXED.getConfig(world.getLevelProperties()).setBiome(
                    Registry.BIOME.getRandom(random)
                )
            ),
            generationSettings
        );
    }
    
    public ChunkGenerator<?> getChunkGenerator3() {
        
        FloatingIslandsChunkGeneratorConfig generationSettings = ChunkGeneratorType.FLOATING_ISLANDS.createSettings();
        generationSettings.setDefaultBlock(Blocks.STONE.getDefaultState());
        generationSettings.setDefaultFluid(Blocks.WATER.getDefaultState());
        generationSettings.withCenter(this.getForcedSpawnPoint());
        
        
        return new StretchedSkylandGenerator(
            world,
            new ChaosBiomeSource(world.getSeed()),
            generationSettings
        
        );
    }
    
    public ChunkGenerator<?> getChunkGenerator4() {
        FloatingIslandsChunkGeneratorConfig generationSettings = ChunkGeneratorType.FLOATING_ISLANDS.createSettings();
        generationSettings.setDefaultBlock(Blocks.STONE.getDefaultState());
        generationSettings.setDefaultFluid(Blocks.WATER.getDefaultState());
        generationSettings.withCenter(this.getForcedSpawnPoint());
    
        return new ErrorTerrainGenerator(
            world,
            new ChaosBiomeSource(world.getSeed()),
            generationSettings
        );
    }
    
    public ChunkGenerator<?> getChunkGenerator5() {
        FloatingIslandsChunkGeneratorConfig generationSettings = ChunkGeneratorType.FLOATING_ISLANDS.createSettings();
        generationSettings.setDefaultBlock(Blocks.STONE.getDefaultState());
        generationSettings.setDefaultFluid(Blocks.WATER.getDefaultState());
        generationSettings.withCenter(this.getForcedSpawnPoint());
        
        return new VoidChunkGenerator(
            world,
            BiomeSourceType.FIXED.applyConfig(
                BiomeSourceType.FIXED.getConfig(world.getLevelProperties()).setBiome(
                    Biomes.THE_VOID
                )
            ),
            generationSettings
        );
    }
    
    
    @Override
    public BlockPos getSpawningBlockInChunk(ChunkPos chunkPos, boolean checkMobSpawnValidity) {
        return null;
    }
    
    @Override
    public BlockPos getTopSpawningBlockPosition(int x, int z, boolean checkMobSpawnValidity) {
        return null;
    }
    
    @Override
    public float getSkyAngle(long worldTime, float partialTicks) {
        double d0 = MathHelper.fractionalPart((double) worldTime / 24000.0D - 0.25D);
        double d1 = 0.5D - Math.cos(d0 * Math.PI) / 2.0D;
        return (float) (d0 * 2.0D + d1) / 3.0F;
    }
    
    @Override
    public boolean hasVisibleSky() {
        return true;
    }
    
    @Environment(EnvType.CLIENT)
    @Override
    public Vec3d getFogColor(float celestialAngle, float partialTicks) {
        float f = MathHelper.cos(celestialAngle * ((float) Math.PI * 2F)) * 2.0F + 0.5F;
        f = MathHelper.clamp(f, 0.0F, 1.0F);
        float f1 = 0.7529412F;
        float f2 = 0.84705883F;
        float f3 = 1.0F;
        f1 = f1 * (f * 0.94F + 0.06F);
        f2 = f2 * (f * 0.94F + 0.06F);
        f3 = f3 * (f * 0.91F + 0.09F);
        return new Vec3d((double) f1, (double) f2, (double) f3);
    }
    
    @Override
    public boolean canPlayersSleep() {
        return true;
    }
    
    @Override
    public boolean isFogThick(int x, int z) {
        return false;
    }
    
    @Override
    public DimensionType getType() {
        return dimensionTypeSupplier.get();
    }
    
    @Environment(EnvType.CLIENT)
    @Override
    public boolean hasGround() {
        return true;
    }
    
    /**
     * the y level at which clouds are rendered.
     */
    @Environment(EnvType.CLIENT)
    @Override
    public float getCloudHeight() {
        return 128.0F;
    }
    
}
