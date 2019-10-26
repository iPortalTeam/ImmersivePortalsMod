package com.qouteall.immersive_portals.ducks;

import net.minecraft.util.math.Vec3d;
import net.minecraft.world.dimension.DimensionType;

public interface IEBackgroundRenderer {
    Vec3d getFogColor();
    
    void setDimensionConstraint(DimensionType dim);
    
    DimensionType getDimensionConstraint();
}
