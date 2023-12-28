package qouteall.imm_ptl.core.portal.shape;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import qouteall.imm_ptl.core.collision.CollisionHelper;
import qouteall.imm_ptl.core.collision.PortalCollisionHandler;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.animation.UnilateralPortalState;
import qouteall.imm_ptl.core.render.FrustumCuller;
import qouteall.imm_ptl.core.render.ViewAreaRenderer;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.my_util.BoxPredicateF;
import qouteall.q_misc_util.my_util.Plane;
import qouteall.q_misc_util.my_util.Range;
import qouteall.q_misc_util.my_util.RayTraceResult;
import qouteall.q_misc_util.my_util.TriangleConsumer;

public final class RectangularPortalShape implements PortalShape {
    public static final RectangularPortalShape INSTANCE = new RectangularPortalShape();
    
    public static void init() {
        PortalShapeSerialization.addSerializer(new PortalShapeSerialization.Serializer<>(
            "rectangular",
            RectangularPortalShape.class,
            s -> new CompoundTag(),
            t -> INSTANCE
        ));
    }
    
    @Override
    public boolean isPlanar() {
        return true;
    }
    
    @Override
    public AABB getBoundingBox(
        UnilateralPortalState portalState, boolean limitSize,
        double boxExpand
    ) {
        double halfW = portalState.width() / 2;
        double halfH = portalState.height() / 2;
        
        if (limitSize) {
            halfW = Math.min(halfW, 64);
            halfH = Math.min(halfH, 64);
        }
        
        return Helper.boundingBoxOfPoints(
            new Vec3[]{
                portalState.transformLocalToGlobal(halfW, halfH, boxExpand),
                portalState.transformLocalToGlobal(halfW, -halfH, boxExpand),
                portalState.transformLocalToGlobal(-halfW, halfH, boxExpand),
                portalState.transformLocalToGlobal(-halfW, -halfH, boxExpand),
                portalState.transformLocalToGlobal(halfW, halfH, -boxExpand),
                portalState.transformLocalToGlobal(halfW, -halfH, -boxExpand),
                portalState.transformLocalToGlobal(-halfW, halfH, -boxExpand),
                portalState.transformLocalToGlobal(-halfW, -halfH, -boxExpand)
            }
        );
    }
    
    @Override
    public double roughDistanceToPortalShape(UnilateralPortalState portalState, Vec3 pos) {
        Vec3 localPos = portalState.transformGlobalToLocal(pos);
        
        double distToRec = Helper.getDistanceToRectangle(
            localPos.x(), localPos.y(),
            -portalState.width() / 2, -portalState.height() / 2,
            portalState.width() / 2, portalState.height() / 2
        );
        
        return Math.sqrt(distToRec * distToRec + localPos.z() * localPos.z());
    }
    
    public @Nullable RayTraceResult raytracePortalShapeByLocalPos(
        UnilateralPortalState portalState,
        Vec3 localFrom, Vec3 localTo, double leniency
    ) {
        double width = portalState.width();
        double height = portalState.height();
        
        if (localFrom.z() > 0 && localTo.z() < 0) {
            double deltaZ = localTo.z() - localFrom.z();
            
            // localFrom + (localTo - localFrom) * t = 0
            // localFrom.z() + deltaZ * t = 0
            // t = -localFrom.z() / deltaZ
            // Note: do not trust GitHub copilot. It may use z as up axis.
            
            double t = -localFrom.z() / deltaZ;
            Vec3 hit = localFrom.add(localTo.subtract(localFrom).scale(t));
            if (Math.abs(hit.x()) < width / 2 + leniency &&
                Math.abs(hit.y()) < height / 2 + leniency
            ) {
                return new RayTraceResult(
                    t, hit, new Vec3(0, 0, 1)
                );
            }
        }
        
        return null;
    }
    
    // the plane's normal points to the "remaining" side
    @Override
    public Plane getOuterClipping(UnilateralPortalState portalState) {
        return new Plane(
            portalState.position(),
            portalState.getNormal()
        );
    }
    
    @Override
    public Plane getInnerClipping(
        UnilateralPortalState thisSideState, UnilateralPortalState otherSideState,
        Portal portal
    ) {
        return new Plane(
            otherSideState.position(),
            otherSideState.getNormal()
        );
    }
    
    @Override
    public PortalShape getFlipped() {
        return this;
    }
    
    @Override
    public PortalShape getReverse() {
        return this;
    }
    
    @Environment(EnvType.CLIENT)
    @Override
    public void renderViewAreaMesh(
        Vec3 portalOriginRelativeToCamera,
        UnilateralPortalState portalState,
        TriangleConsumer vertexOutput,
        boolean isGlobalPortal
    ) {
        final double w = Math.min(portalState.width(), 23333);
        final double h = Math.min(portalState.height(), 23333);
        
        Vec3 localXAxis = portalState.getAxisW().scale(w / 2);
        Vec3 localYAxis = portalState.getAxisH().scale(h / 2);
        
        ViewAreaRenderer.outputFullQuad(
            vertexOutput, portalOriginRelativeToCamera, localXAxis, localYAxis
        );
    }
    
    @Override
    public boolean roughTestVisibility(
        UnilateralPortalState portalState,
        Vec3 cameraPos
    ) {
        Vec3 localPos = portalState.transformGlobalToLocal(cameraPos);
        
        return localPos.z() > 0;
    }
    
    @Override
    public boolean canCollideWith(
        Portal portal, UnilateralPortalState portalState, Vec3 entityEyePos,
        AABB entityBoundingBox
    ) {
        boolean inFrontOfPortal = portalState.transformGlobalToLocal(entityEyePos).z() > 0;
        
        if (!inFrontOfPortal) {
            return false;
        }
        
        return isBoxInPortalProjection(portalState, entityBoundingBox);
    }
    
    @Override
    public boolean isLocalBoxInPortalProjection(
        UnilateralPortalState portalState,
        double minX, double minY, double minZ, double maxX, double maxY, double maxZ
    ) {
        double halfWidth = portalState.width() / 2;
        double halfHeight = portalState.height() / 2;
        
        return Range.rangeIntersects(-halfWidth, halfWidth, minX, maxX)
            && Range.rangeIntersects(-halfHeight, halfHeight, minY, maxY);
    }
    
    @Override
    public Vec3 getMovementForPushingEntityOutOfPortal(
        Portal portal, UnilateralPortalState portalState, Entity entity,
        Vec3 attemptedMove
    ) {
        AABB originalBoundingBox = entity.getBoundingBox();
        
        return PortalCollisionHandler.getMovementForPushingEntityOutOfPortal(
            attemptedMove, portalState.position(), portalState.getNormal(), originalBoundingBox
        );
    }
    
    @Override
    public PortalShape cloneIfNecessary() {
        // the object does not contain any mutable field
        // no need to clone
        return this;
    }
    
    @Environment(EnvType.CLIENT)
    @Override
    public boolean canDoOuterFrustumCulling() {
        return true;
    }
    
    @Environment(EnvType.CLIENT)
    @Override
    public BoxPredicateF getInnerFrustumCullingFunc(
        Portal portal,
        Vec3 cameraPos
    ) {
        return FrustumCuller.getFlatPortalInnerFrustumCullingFunc(
            portal, cameraPos
        );
    }
    
    @Override
    public BoxPredicateF getOuterFrustumCullingFunc(
        Portal portal,
        Vec3 cameraPos
    ) {
        return FrustumCuller.getFlatPortalOuterFrustumCullingFunc(
            portal, cameraPos
        );
    }
    
    @Override
    public VoxelShape getThisSideCollisionExclusion(
        UnilateralPortalState portalState
    ) {
        AABB thinAreaBox = getBoundingBox(portalState, true, 0);
        Vec3 reaching = portalState.getNormal().scale(-10);
        AABB exclusion = thinAreaBox.minmax(thinAreaBox.move(reaching));
        return Shapes.create(exclusion);
    }
    
    @Override
    public @Nullable AABB outerClipBox(Portal portal, AABB box) {
        return CollisionHelper.clipBox(
            box, portal.getOriginPos(), portal.getNormal()
        );
    }
}
