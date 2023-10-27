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

public final class BoxPortalShape implements PortalShape {
    
    public static final BoxPortalShape FACING_OUTWARDS = new BoxPortalShape(true);
    public static final BoxPortalShape FACING_INWARDS = new BoxPortalShape(false);
    
    public final boolean facingOutwards;
    
    public static void init() {
        PortalShapeSerialization.addSerializer(new PortalShapeSerialization.Serializer<>(
            "box",
            BoxPortalShape.class,
            BoxPortalShape::serialize,
            BoxPortalShape::deserialize
        ));
    }
    
    private static BoxPortalShape deserialize(CompoundTag tag) {
        boolean facingOutwards1 = tag.getBoolean("facingOutwards");
        if (facingOutwards1) {
            return FACING_OUTWARDS;
        }
        else {
            return FACING_INWARDS;
        }
    }
    
    public static CompoundTag serialize(BoxPortalShape boxShape) {
        CompoundTag compoundTag = new CompoundTag();
        compoundTag.putBoolean("facingOutwards", boxShape.facingOutwards);
        return compoundTag;
    }
    
    private BoxPortalShape(boolean facingOutwards) {this.facingOutwards = facingOutwards;}
    
    @Override
    public boolean isPlanar() {
        return false;
    }
    
    @Override
    public AABB getBoundingBox(UnilateralPortalState portalState) {
        double halfW = portalState.width() / 2;
        double halfH = portalState.height() / 2;
        double halfT = portalState.thickness() / 2;
        return Helper.boundingBoxOfPoints(
            new Vec3[]{
                portalState.transformLocalToGlobal(-halfW, -halfH, -halfT),
                portalState.transformLocalToGlobal(-halfW, -halfH, halfT),
                portalState.transformLocalToGlobal(-halfW, halfH, -halfT),
                portalState.transformLocalToGlobal(-halfW, halfH, halfT),
                portalState.transformLocalToGlobal(halfW, -halfH, -halfT),
                portalState.transformLocalToGlobal(halfW, -halfH, halfT),
                portalState.transformLocalToGlobal(halfW, halfH, -halfT),
                portalState.transformLocalToGlobal(halfW, halfH, halfT)
            }
        );
    }
    
    @Override
    public double distanceToPortalShape(UnilateralPortalState portalState, Vec3 pos) {
        Vec3 localPos = portalState.transformGlobalToLocal(pos);
        
        double dx = Helper.getDistanceToRange(
            -portalState.width() / 2, portalState.width() / 2, localPos.x()
        );
        double dy = Helper.getDistanceToRange(
            -portalState.height() / 2, portalState.height() / 2, localPos.y()
        );
        double dz = Helper.getDistanceToRange(
            -portalState.thickness() / 2, portalState.thickness() / 2, localPos.z()
        );
        
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
    
    @Override
    public @Nullable RayTraceResult raytracePortalShapeByLocalPos(
        UnilateralPortalState portalState, Vec3 localFrom, Vec3 localTo, double leniency
    ) {
        Vec3 lineVec = localTo.subtract(localFrom);
        
        RayTraceResult rayTraceResult = Helper.raytraceAABB(
            facingOutwards,
            -portalState.width() / 2,
            -portalState.height() / 2,
            -portalState.thickness() / 2,
            portalState.width() / 2,
            portalState.height() / 2,
            portalState.thickness() / 2,
            localFrom.x(), localFrom.y(), localFrom.z(),
            lineVec.x(), lineVec.y(), lineVec.z()
        );
        
        return rayTraceResult;
    }
    
    @Override
    public @Nullable Plane getOuterClipping(UnilateralPortalState portalState) {
        return null;
    }
    
    @Override
    public @Nullable Plane getInnerClipping(
        UnilateralPortalState thisSideState, UnilateralPortalState otherSideState
    ) {
        return null;
    }
    
    @Override
    public PortalShape getFlipped() {
        if (facingOutwards) {
            return FACING_INWARDS;
        }
        else {
            return FACING_OUTWARDS;
        }
    }
    
    @Override
    public PortalShape getReverse() {
        return getFlipped();
    }
    
    @Override
    public boolean roughTestVisibility(
        UnilateralPortalState portalState,
        Vec3 cameraPos
    ) {
        Vec3 localPos = portalState.transformGlobalToLocal(cameraPos);
        
        boolean in = localPos.x() > -portalState.width() / 2 &&
            localPos.x() < portalState.width() / 2 &&
            localPos.y() > -portalState.height() / 2 &&
            localPos.y() < portalState.height() / 2 &&
            localPos.z() > -portalState.thickness() / 2 &&
            localPos.z() < portalState.thickness() / 2;
        
        if (facingOutwards) {
            return in;
        }
        else {
            return !in;
        }
    }
    
    @Override
    public void renderViewAreaMesh(
        Vec3 portalOriginRelativeToCamera,
        UnilateralPortalState portalState,
        TriangleConsumer vertexOutput,
        boolean isGlobalPortal
    ) {
        Vec3 localHX = portalState.getAxisW().scale(portalState.width() / 2);
        Vec3 localHY = portalState.getAxisH().scale(portalState.height() / 2);
        Vec3 localHZ = portalState.getNormal().scale(portalState.thickness() / 2);
        
        ViewAreaRenderer.outputFullQuad(
            vertexOutput,
            portalOriginRelativeToCamera.add(localHX),
            facingOutwards ? localHY : localHZ,
            facingOutwards ? localHZ : localHY
        );
        
        ViewAreaRenderer.outputFullQuad(
            vertexOutput,
            portalOriginRelativeToCamera.subtract(localHX),
            facingOutwards ? localHZ : localHY,
            facingOutwards ? localHY : localHZ
        );
        
        ViewAreaRenderer.outputFullQuad(
            vertexOutput,
            portalOriginRelativeToCamera.add(localHY),
            facingOutwards ? localHZ : localHX,
            facingOutwards ? localHX : localHZ
        );
        
        ViewAreaRenderer.outputFullQuad(
            vertexOutput,
            portalOriginRelativeToCamera.subtract(localHY),
            facingOutwards ? localHX : localHZ,
            facingOutwards ? localHZ : localHX
        );
        
        ViewAreaRenderer.outputFullQuad(
            vertexOutput,
            portalOriginRelativeToCamera.add(localHZ),
            facingOutwards ? localHX : localHY,
            facingOutwards ? localHY : localHX
        );
        
        ViewAreaRenderer.outputFullQuad(
            vertexOutput,
            portalOriginRelativeToCamera.subtract(localHZ),
            facingOutwards ? localHY : localHX,
            facingOutwards ? localHX : localHY
        );
    }
}
