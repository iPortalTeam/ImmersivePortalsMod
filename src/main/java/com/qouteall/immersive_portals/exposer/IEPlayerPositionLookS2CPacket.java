package com.qouteall.immersive_portals.exposer;

import net.minecraft.world.dimension.DimensionType;

public interface IEPlayerPositionLookS2CPacket {
    DimensionType getPlayerDimension();
    
    void setPlayerDimension(DimensionType dimension);
}
