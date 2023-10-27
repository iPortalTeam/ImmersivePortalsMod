package qouteall.imm_ptl.core.portal.shape;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import qouteall.imm_ptl.core.portal.animation.UnilateralPortalState;
import qouteall.q_misc_util.my_util.Plane;
import qouteall.q_misc_util.my_util.RayTraceResult;
import qouteall.q_misc_util.my_util.TriangleConsumer;

import java.util.List;

public interface PortalShape {
    public boolean isPlanar();
    
    public AABB getBoundingBox(
        UnilateralPortalState portalState
    );
    
    public double distanceToPortalShape(
        UnilateralPortalState portalState, Vec3 pos
    );
    
    public @Nullable RayTraceResult raytracePortalShapeByLocalPos(
        UnilateralPortalState portalState,
        Vec3 localFrom, Vec3 localTo, double leniency
    );
    
    public default @Nullable RayTraceResult raytracePortalShape(
        UnilateralPortalState portalState,
        Vec3 from, Vec3 to,
        double leniency
    ) {
        Vec3 localFrom = portalState.transformGlobalToLocal(from);
        Vec3 localTo = portalState.transformGlobalToLocal(to);
        
        RayTraceResult hit = raytracePortalShapeByLocalPos(
            portalState, localFrom, localTo, leniency
        );
        
        if (hit != null) {
            return new RayTraceResult(
                hit.t(),
                portalState.transformLocalToGlobal(hit.hitPos()),
                portalState.transformVecLocalToGlobal(hit.surfaceNormal())
            );
        }
        
        return null;
    }
    
    public @Nullable Plane getOuterClipping(
        UnilateralPortalState portalState
    );
    
    public @Nullable Plane getInnerClipping(
        UnilateralPortalState thisSideState,
        UnilateralPortalState otherSideState
    );
    
    public default @Nullable List<Plane> getNearbyPortalPlanes(
        UnilateralPortalState portalState,
        AABB box
    ) {
        Plane outerClipping = getOuterClipping(portalState);
        if (outerClipping != null) {
            return List.of(outerClipping);
        }
        else {
            return null;
        }
    }
    
    public PortalShape getFlipped();
    
    public PortalShape getReverse();
    
    public boolean roughTestVisibility(
        UnilateralPortalState portalState,
        Vec3 cameraPos
    );
    
    public void renderViewAreaMesh(
        Vec3 portalOriginRelativeToCamera,
        UnilateralPortalState portalState,
        TriangleConsumer vertexOutput,
        boolean isGlobalPortal
    );
    
}
