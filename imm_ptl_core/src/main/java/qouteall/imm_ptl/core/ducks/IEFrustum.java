package qouteall.imm_ptl.core.ducks;

import net.minecraft.world.phys.Vec3;

public interface IEFrustum {
    boolean canDetermineInvisible(double minX, double minY, double minZ, double maxX, double maxY, double maxZ);
    
    Vec3 ip_getViewVec3();
}
