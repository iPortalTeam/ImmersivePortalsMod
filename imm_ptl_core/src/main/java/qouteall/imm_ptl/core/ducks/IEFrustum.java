package qouteall.imm_ptl.core.ducks;

import net.minecraft.world.phys.Vec3;

public interface IEFrustum {
    boolean ip_canDetermineInvisibleWithCamCoord(
        float minX, float minY, float minZ, float maxX, float maxY, float maxZ
    );
    
    Vec3 ip_getViewVec3();
}
