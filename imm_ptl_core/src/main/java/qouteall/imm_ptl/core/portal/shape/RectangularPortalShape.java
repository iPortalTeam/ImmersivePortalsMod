package qouteall.imm_ptl.core.portal.shape;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import qouteall.imm_ptl.core.portal.animation.UnilateralPortalState;
import qouteall.imm_ptl.core.render.ViewAreaRenderer;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.my_util.Plane;
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
    public AABB getBoundingBox(UnilateralPortalState portalState) {
        double thickness = 0.02;
        double halfW = portalState.width() / 2;
        double halfH = portalState.height() / 2;
        return Helper.boundingBoxOfPoints(
            new Vec3[]{
                portalState.transformLocalToGlobal(halfW, halfH, thickness),
                portalState.transformLocalToGlobal(halfW, -halfH, thickness),
                portalState.transformLocalToGlobal(-halfW, halfH, thickness),
                portalState.transformLocalToGlobal(-halfW, -halfH, thickness),
                portalState.transformLocalToGlobal(halfW, halfH, -thickness),
                portalState.transformLocalToGlobal(halfW, -halfH, -thickness),
                portalState.transformLocalToGlobal(-halfW, halfH, -thickness),
                portalState.transformLocalToGlobal(-halfW, -halfH, -thickness)
            }
        );
    }
    
    @Override
    public double distanceToPortalShape(UnilateralPortalState portalState, Vec3 pos) {
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
        
        if (localFrom.y() > 0 && localTo.y() < 0) {
            double deltaY = localTo.y() - localFrom.y();
            
            // localFrom + (localTo - localFrom) * t = 0
            // localFrom.y() + deltaY * t = 0
            // t = -localFrom.y() / deltaY
            
            double t = -localFrom.y() / deltaY;
            Vec3 hit = localFrom.add(localTo.subtract(localFrom).scale(t));
            if (Math.abs(hit.x()) < width / 2 + leniency &&
                Math.abs(hit.z()) < height / 2 + leniency
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
        UnilateralPortalState thisSideState, UnilateralPortalState otherSideState
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
}
