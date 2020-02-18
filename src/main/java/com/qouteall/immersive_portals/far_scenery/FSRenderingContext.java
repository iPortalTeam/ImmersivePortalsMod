package com.qouteall.immersive_portals.far_scenery;

import net.minecraft.util.math.Vec3d;

public class FSRenderingContext {
    
    public static boolean isRenderingScenery = false;
    public static Vec3d cameraPos = Vec3d.ZERO;
    public static double nearPlaneDistance = 100;
    public static boolean isFarSceneryEnabled = false;
    public static double[] cullingEquation;
}
