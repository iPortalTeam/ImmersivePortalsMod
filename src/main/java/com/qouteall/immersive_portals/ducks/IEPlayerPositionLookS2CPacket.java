package com.qouteall.immersive_portals.ducks;

import net.minecraft.world.dimension.DimensionType;

public interface IEPlayerPositionLookS2CPacket {
    DimensionType getPlayerDimension();
    
    void setPlayerDimension(DimensionType dimension);
}
