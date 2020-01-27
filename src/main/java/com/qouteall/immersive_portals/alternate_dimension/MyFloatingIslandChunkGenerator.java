package com.qouteall.immersive_portals.alternate_dimension;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.world.IWorld;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.gen.chunk.FloatingIslandsChunkGenerator;
import net.minecraft.world.gen.chunk.FloatingIslandsChunkGeneratorConfig;

public class MyFloatingIslandChunkGenerator extends FloatingIslandsChunkGenerator {
    private final BlockState AIR;
    
    private double wxx;
    private double wzx;
    private double wxz;
    private double wzz;
    
    public MyFloatingIslandChunkGenerator(
        IWorld iWorld,
        BiomeSource biomeSource,
        FloatingIslandsChunkGeneratorConfig floatingIslandsChunkGeneratorConfig
    ) {
        super(iWorld, biomeSource, floatingIslandsChunkGeneratorConfig);
        AIR = Blocks.AIR.getDefaultState();

//        long seed = iWorld.getSeed();
//        double theta = ((seed % 23333) / 23333.0) * Math.PI * 2;
//        wxx = Math.sin(theta);
//        wzx = Math.cos(theta);
//        wxz = Math.sin(theta + (Math.PI / 2));
//        wzz = Math.cos(theta + (Math.PI / 2));
    }
    
    protected int transformX(int x, int z) {
        return x / 2;
    }
    
    protected int transformZ(int x, int z) {
        return z * 10;
    }
    
    @Override
    protected void sampleNoiseColumn(
        double[] buffer,
        int x,
        int z,
        double d,
        double e,
        double f,
        double g,
        int i,
        int j
    ) {
        super.sampleNoiseColumn(buffer,
            transformX(x, z), transformZ(x, z),
            d, e, f, g, i, j
        );

//        IESurfaceChunkGenerator ie = (IESurfaceChunkGenerator) this;
//
//        double[] ds = this.computeNoiseRange(x, z);
//        double h = ds[0];
//        double k = ds[1];
//        double l = this.method_16409();
//        double m = this.method_16410();
//
//        for(int n = 0; n < this.getNoiseSizeY(); ++n) {
//            double o = ie.sampleNoise_(x, n, z, d, e, f, g);
//            o -= this.computeNoiseFalloff(h, k, n);
//            if ((double)n > l) {
//                o = MathHelper.clampedLerp(o, (double)j, ((double)n - l) / (double)i);
//            } else if ((double)n < m) {
//                o = MathHelper.clampedLerp(o, -30.0D, (m - (double)n) / (m - 1.0D));
//            }
//
//            buffer[n] = o;
//        }
    
    }
    
}
