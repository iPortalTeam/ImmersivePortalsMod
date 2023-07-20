package qouteall.imm_ptl.core.portal.util;

import net.minecraft.world.phys.Vec3;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.animation.UnilateralPortalState;

public record PortalLocalXY(double localX, double localY) {
    
    public static PortalLocalXY fromPos(Portal portal, Vec3 pos) {
        Vec3 delta = pos.subtract(portal.getOriginPos());
        return fromOffset(portal, delta);
    }
    
    public static PortalLocalXY fromPos(UnilateralPortalState state, Vec3 pos) {
        Vec3 delta = pos.subtract(state.position());
        return fromOffset(state, delta);
    }
    
    public static PortalLocalXY fromOffset(Portal portal, Vec3 offset) {
        return new PortalLocalXY(
            portal.axisW.dot(offset), portal.axisH.dot(offset)
        );
    }
    
    public static PortalLocalXY fromOffset(UnilateralPortalState state, Vec3 offset) {
        Vec3 result = state.orientation().getConjugated().rotate(offset);
        return new PortalLocalXY(result.x, result.y);
    }
    
    public Vec3 getOffset(Portal portal) {
        return portal.axisW.scale(localX).add(portal.axisH.scale(localY));
    }
    
    public Vec3 getOffset(UnilateralPortalState state) {
        return state.orientation().rotate(new Vec3(localX, localY, 0));
    }
    
    public Vec3 getPos(Portal portal) {
        return portal.getOriginPos().add(getOffset(portal));
    }
    
    public Vec3 getPos(UnilateralPortalState state) {
        return state.position().add(getOffset(state));
    }
}
