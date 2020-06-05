package com.qouteall.immersive_portals.alternate_dimension;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.class_5284;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.gen.chunk.FloatingIslandsChunkGenerator;

public class StretchedSkylandGenerator extends FloatingIslandsChunkGenerator {
    private final BlockState AIR;
    
    private double wxx;
    private double wzx;
    private double wxz;
    private double wzz;
    
    public StretchedSkylandGenerator(
        BiomeSource biomeSource,
        long seed,
        class_5284 config
    ) {
        super(biomeSource, seed, config);
        AIR = Blocks.AIR.getDefaultState();
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
