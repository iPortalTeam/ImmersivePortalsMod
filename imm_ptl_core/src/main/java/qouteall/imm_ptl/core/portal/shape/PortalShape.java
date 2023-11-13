package qouteall.imm_ptl.core.portal.shape;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.animation.UnilateralPortalState;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.my_util.BoxPredicateF;
import qouteall.q_misc_util.my_util.Plane;
import qouteall.q_misc_util.my_util.RayTraceResult;
import qouteall.q_misc_util.my_util.TriangleConsumer;

import java.util.List;

public interface PortalShape {
    public boolean isPlanar();
    
    /**
     * @param limitSize true when it's not a global portal.
     *                  having too big bounding box cause lag
     * @param boxExpand
     */
    public AABB getBoundingBox(
        UnilateralPortalState portalState, boolean limitSize,
        double boxExpand
    );
    
    public double roughDistanceToPortalShape(
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
    
    @Environment(EnvType.CLIENT)
    public void renderViewAreaMesh(
        Vec3 portalOriginRelativeToCamera,
        UnilateralPortalState portalState,
        TriangleConsumer vertexOutput,
        boolean isGlobalPortal
    );
    
    public boolean canCollideWith(
        Portal portal,
        UnilateralPortalState portalState, Entity entity,
        float partialTick
    );
    
    public default boolean isBoxInPortalProjection(
        UnilateralPortalState portalState, AABB box
    ) {
        Vec3[] vertexes = Helper.eightVerticesOf(box);
        
        Vec3 originPos = portalState.position();
        
        Vec3[] transformed = new Vec3[vertexes.length];
        
        for (int i = 0; i < transformed.length; i++) {
            transformed[i] = portalState.transformGlobalToLocal(vertexes[i]);
        }
        
        double minX = transformed[0].x();
        double maxX = minX;
        double minY = transformed[0].y();
        double maxY = minY;
        double minZ = transformed[0].z();
        double maxZ = minZ;
        
        for (int i = 1; i < 8; i++) {
            Vec3 v = transformed[i];
            minX = Math.min(minX, v.x());
            maxX = Math.max(maxX, v.x());
            minY = Math.min(minY, v.y());
            maxY = Math.max(maxY, v.y());
            minZ = Math.min(minZ, v.z());
            maxZ = Math.max(maxZ, v.z());
        }
        
        return isLocalBoxInPortalProjection(
            portalState, minX, minY, minZ, maxX, maxY, maxZ
        );
    }
    
    public boolean isLocalBoxInPortalProjection(
        UnilateralPortalState portalState,
        double minX, double minY, double minZ,
        double maxX, double maxY, double maxZ
    );
    
    public default @Nullable VoxelShape getThisSideCollisionExclusion(
        UnilateralPortalState portalState
    ) {
        return null;
    }
    
    /**
     * Entities are pushed out when the other side of the portal is not yet loaded.
     */
    public Vec3 getMovementForPushingEntityOutOfPortal(
        Portal portal, UnilateralPortalState portalState,
        Entity entity, Vec3 attemptedMove
    );
    
    public PortalShape cloneIfNecessary();
    
    @Environment(EnvType.CLIENT)
    public default boolean canDoOuterFrustumCulling() {
        return false;
    }
    
    // the func returning true for culled
    @Environment(EnvType.CLIENT)
    public default @Nullable BoxPredicateF getInnerFrustumCullingFunc(
        Portal portal, Vec3 cameraPos
    ) {
        return null;
    }
    
    @Environment(EnvType.CLIENT)
    public default @Nullable BoxPredicateF getOuterFrustumCullingFunc(
        Portal portal, Vec3 cameraPos
    ) {
        return null;
    }
}
