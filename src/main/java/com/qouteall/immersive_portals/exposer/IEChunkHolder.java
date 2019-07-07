package com.qouteall.immersive_portals.exposer;

import net.minecraft.world.dimension.DimensionType;

public interface IEChunkHolder {
    DimensionType getDimension();
    
    void setDimension(DimensionType dimension);
}
