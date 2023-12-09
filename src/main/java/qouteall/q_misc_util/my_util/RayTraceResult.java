package qouteall.q_misc_util.my_util;

import net.minecraft.world.phys.Vec3;

public record RayTraceResult(
    double t, Vec3 hitPos, Vec3 surfaceNormal
) {}
