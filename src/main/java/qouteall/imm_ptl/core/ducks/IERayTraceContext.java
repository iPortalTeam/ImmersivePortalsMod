package qouteall.imm_ptl.core.ducks;

import net.minecraft.world.phys.Vec3;

public interface IERayTraceContext {
    IERayTraceContext ip_setStart(Vec3 newStart);

    IERayTraceContext ip_setEnd(Vec3 newEnd);
}
