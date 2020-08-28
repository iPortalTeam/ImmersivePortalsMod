package com.qouteall.immersive_portals.ducks;

import net.minecraft.util.math.Vec3d;

public interface IERayTraceContext {
    IERayTraceContext setStart(Vec3d newStart);

    IERayTraceContext setEnd(Vec3d newEnd);
}
