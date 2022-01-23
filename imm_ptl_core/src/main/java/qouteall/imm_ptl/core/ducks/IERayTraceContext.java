package qouteall.imm_ptl.core.ducks;

import net.minecraft.world.phys.Vec3;

public interface IERayTraceContext {
    IERayTraceContext setStart(Vec3 newStart);

    IERayTraceContext setEnd(Vec3 newEnd);
}
