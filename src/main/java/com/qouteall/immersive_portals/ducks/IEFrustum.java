package com.qouteall.immersive_portals.ducks;

public interface IEFrustum {
    boolean canDetermineInvisible(double minX, double minY, double minZ, double maxX, double maxY, double maxZ);
}
