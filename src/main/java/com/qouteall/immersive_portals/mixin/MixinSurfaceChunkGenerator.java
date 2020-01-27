package com.qouteall.immersive_portals.mixin;

import com.qouteall.immersive_portals.ducks.IESurfaceChunkGenerator;
import net.minecraft.world.gen.chunk.SurfaceChunkGenerator;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(SurfaceChunkGenerator.class)
public abstract class MixinSurfaceChunkGenerator implements IESurfaceChunkGenerator {
    @Shadow
    @Final
    private int verticalNoiseResolution;
    @Shadow
    @Final
    private int horizontalNoiseResolution;
    @Shadow
    @Final
    private int noiseSizeX;
    @Shadow
    @Final
    private int noiseSizeY;
    @Shadow
    @Final
    private int noiseSizeZ;
    
    
    @Shadow
    protected abstract double sampleNoise(
        int x,
        int y,
        int z,
        double d,
        double e,
        double f,
        double g
    );
    
    @Override
    public int get_verticalNoiseResolution() {
        return verticalNoiseResolution;
    }
    
    @Override
    public int get_horizontalNoiseResolution() {
        return horizontalNoiseResolution;
    }
    
    @Override
    public int get_noiseSizeX() {
        return noiseSizeX;
    }
    
    @Override
    public int get_noiseSizeY() {
        return noiseSizeY;
    }
    
    @Override
    public int get_noiseSizeZ() {
        return noiseSizeZ;
    }
    
    @Override
    public double sampleNoise_(int x, int y, int z, double d, double e, double f, double g) {
        return sampleNoise(x, y, z, d, e, f, g);
    }
}
