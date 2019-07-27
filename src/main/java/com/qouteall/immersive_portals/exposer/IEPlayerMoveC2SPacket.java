package com.qouteall.immersive_portals.exposer;

import net.minecraft.world.dimension.DimensionType;

public interface IEPlayerMoveC2SPacket {
    DimensionType getPlayerDimension();
    
    void setPlayerDimension(DimensionType dim);
}
