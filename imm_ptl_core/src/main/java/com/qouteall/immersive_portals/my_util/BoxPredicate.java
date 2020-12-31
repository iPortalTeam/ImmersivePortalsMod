package com.qouteall.immersive_portals.my_util;

public interface BoxPredicate {
    public static BoxPredicate nonePredicate =
        (double minX, double minY, double minZ, double maxX, double maxY, double maxZ) -> false;
    
    boolean test(double minX, double minY, double minZ, double maxX, double maxY, double maxZ);
}
