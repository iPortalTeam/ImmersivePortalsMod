package qouteall.imm_ptl.core.ducks;

import net.minecraft.util.math.Vec3d;

public interface IERayTraceContext {
    IERayTraceContext setStart(Vec3d newStart);

    IERayTraceContext setEnd(Vec3d newEnd);
}
