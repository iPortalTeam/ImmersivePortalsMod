package com.qouteall.immersive_portals.ducks;

public interface IESurfaceChunkGenerator {
    int get_verticalNoiseResolution();
    
    int get_horizontalNoiseResolution();
    
    int get_noiseSizeX();
    
    int get_noiseSizeY();
    
    int get_noiseSizeZ();
    
    double sampleNoise_(int x, int y, int z, double d, double e, double f, double g);
}
