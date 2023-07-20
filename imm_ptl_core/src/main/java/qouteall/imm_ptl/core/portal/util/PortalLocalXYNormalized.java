package qouteall.imm_ptl.core.portal.util;

import net.minecraft.world.phys.Vec3;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.animation.UnilateralPortalState;

// nx and ny are from 0 to 1
public record PortalLocalXYNormalized(double nx, double ny) {
    public Vec3 getOffset(UnilateralPortalState state) {
        return state.orientation().rotate(new Vec3(
            (nx - 0.5) * state.width(), (ny - 0.5) * state.height(), 0
        ));
    }
    
    public Vec3 getPos(UnilateralPortalState state) {
        return state.position().add(getOffset(state));
    }
    
    public Vec3 getOffset(Portal portal) {
        return portal.axisW.scale((nx - 0.5) * portal.width)
            .add(portal.axisH.scale((ny - 0.5) * portal.height));
    }
    
    public Vec3 getPos(Portal portal) {
        return portal.getOriginPos().add(getOffset(portal));
    }
    
    public static PortalLocalXYNormalized fromOffset(UnilateralPortalState state, Vec3 offset) {
        Vec3 localPos = state.orientation().getConjugated().rotate(offset);
        return new PortalLocalXYNormalized(
            localPos.x / state.width() + 0.5,
            localPos.y / state.height() + 0.5
        );
    }
    
    public static PortalLocalXYNormalized fromPos(UnilateralPortalState state, Vec3 pos) {
        return fromOffset(state, pos.subtract(state.position()));
    }
    
    public static PortalLocalXYNormalized fromOffset(Portal portal, Vec3 offset) {
        return new PortalLocalXYNormalized(
            portal.axisW.dot(offset) / portal.width + 0.5,
            portal.axisH.dot(offset) / portal.height + 0.5
        );
    }
    
    public static PortalLocalXYNormalized fromPos(Portal portal, Vec3 pos) {
        return fromOffset(portal, pos.subtract(portal.getOriginPos()));
    }
    
    public PortalLocalXYNormalized clamp() {
        return new PortalLocalXYNormalized(
            Math.min(Math.max(nx, 0), 1),
            Math.min(Math.max(ny, 0), 1)
        );
    }
    
    public PortalLocalXYNormalized snapToGrid(int gridCount) {
        return new PortalLocalXYNormalized(
            ((double) Math.round(nx * gridCount)) / gridCount,
            ((double) Math.round(ny * gridCount)) / gridCount
        );
    }
    
    public boolean isValid() {
        return nx >= 0 && nx <= 1 && ny >= 0 && ny <= 1;
    }
    
    public boolean isCloseTo(PortalLocalXYNormalized another, double maxDistance) {
        double distSq = (nx - another.nx) * (nx - another.nx) + (ny - another.ny) * (ny - another.ny);
        return distSq < maxDistance * maxDistance;
    }
    
    public PortalLocalXYNormalized add(PortalLocalXYNormalized another) {
        return new PortalLocalXYNormalized(nx + another.nx, ny + another.ny);
    }
    
    public PortalLocalXYNormalized subtract(PortalLocalXYNormalized another) {
        return new PortalLocalXYNormalized(nx - another.nx, ny - another.ny);
    }
}
