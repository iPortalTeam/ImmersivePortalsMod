package qouteall.imm_ptl.core.portal.shape;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import qouteall.imm_ptl.core.portal.animation.UnilateralPortalState;
import qouteall.imm_ptl.core.render.ViewAreaRenderer;
import qouteall.q_misc_util.my_util.Mesh2D;
import qouteall.q_misc_util.my_util.Plane;
import qouteall.q_misc_util.my_util.RayTraceResult;
import qouteall.q_misc_util.my_util.TriangleConsumer;

public final class SpecialFlatPortalShape implements PortalShape {
    public final @NotNull Mesh2D mesh;
    
    public SpecialFlatPortalShape(@NotNull Mesh2D mesh) {
        this.mesh = mesh;
        this.mesh.enableTriangleLookup();
    }
    
    public static void init() {
        PortalShapeSerialization.addSerializer(
            new PortalShapeSerialization.Serializer<>(
                "specialFlat",
                SpecialFlatPortalShape.class,
                SpecialFlatPortalShape::serialize,
                SpecialFlatPortalShape::deserialize
            )
        );
    }
    
    @NotNull
    private CompoundTag serialize() {
        CompoundTag compoundTag = new CompoundTag();
        compoundTag.put("shape", mesh.toTag());
        return compoundTag;
    }
    
    private static @Nullable SpecialFlatPortalShape deserialize(CompoundTag tag) {
        Mesh2D m = Mesh2D.fromTag(tag.getCompound("shape"));
        if (m == null) {
            return null;
        }
        return new SpecialFlatPortalShape(m);
    }
    
    @Override
    public boolean isPlanar() {
        return true;
    }
    
    @Override
    public AABB getBoundingBox(UnilateralPortalState portalState) {
        return RectangularPortalShape.INSTANCE.getBoundingBox(portalState);
    }
    
    // it does not calculate "real" distance here
    @Override
    public double distanceToPortalShape(UnilateralPortalState portalState, Vec3 pos) {
        return RectangularPortalShape.INSTANCE.distanceToPortalShape(portalState, pos);
    }
    
    @Override
    public @Nullable RayTraceResult raytracePortalShapeByLocalPos(
        UnilateralPortalState portalState,
        Vec3 localFrom, Vec3 localTo, double leniency
    ) {
        RayTraceResult roughRayTrace = RectangularPortalShape.INSTANCE
            .raytracePortalShapeByLocalPos(
                portalState, localFrom, localTo, leniency
            );
        
        if (roughRayTrace == null) {
            return null;
        }
        
        double localX = roughRayTrace.hitPos().x();
        double localY = roughRayTrace.hitPos().y();
        double halfWidth = portalState.width() / 2;
        double halfHeight = portalState.height() / 2;
        
        double boxR = Math.max(leniency, 0.00001);
        double nx = localX / halfWidth;
        double ny = localY / halfHeight;
        boolean intersectWithMesh = mesh.boxIntersects(
            nx - boxR, ny - boxR,
            nx + boxR, ny + boxR
        );
        
        if (intersectWithMesh) {
            return roughRayTrace;
        }
        else {
            return null;
        }
    }
    
    @Override
    public Plane getOuterClipping(UnilateralPortalState portalState) {
        return RectangularPortalShape.INSTANCE.getOuterClipping(portalState);
    }
    
    @Override
    public Plane getInnerClipping(
        UnilateralPortalState thisSideState, UnilateralPortalState otherSideState
    ) {
        return RectangularPortalShape.INSTANCE.getInnerClipping(thisSideState, otherSideState);
    }
    
    @Override
    public PortalShape getFlipped() {
        Mesh2D newMesh = new Mesh2D();
        
        for (int ti = 0; ti < mesh.getStoredTriangleNum(); ti++) {
            if (mesh.isTriangleValid(ti)) {
                int p0Index = mesh.trianglePointIndexes.getInt(ti * 3);
                int p1Index = mesh.trianglePointIndexes.getInt(ti * 3 + 1);
                int p2Index = mesh.trianglePointIndexes.getInt(ti * 3 + 2);
                
                newMesh.addTriangle(
                    -mesh.pointCoords.getDouble(p0Index * 2),
                    mesh.pointCoords.getDouble(p0Index * 2 + 1),
                    -mesh.pointCoords.getDouble(p1Index * 2),
                    mesh.pointCoords.getDouble(p1Index * 2 + 1),
                    -mesh.pointCoords.getDouble(p2Index * 2),
                    mesh.pointCoords.getDouble(p2Index * 2 + 1)
                );
            }
        }
        
        return new SpecialFlatPortalShape(newMesh);
    }
    
    @Override
    public PortalShape getReverse() {
        return getFlipped();
    }
    
    @Override
    public void renderViewAreaMesh(
        Vec3 portalOriginRelativeToCamera,
        UnilateralPortalState portalState,
        TriangleConsumer vertexOutput,
        boolean isGlobalPortal
    ) {
        double halfWidth = portalState.width() / 2;
        double halfHeight = portalState.height() / 2;
        
        Vec3 localXAxis = portalState.getAxisW().scale(halfWidth);
        Vec3 localYAxis = portalState.getAxisH().scale(halfHeight);
        
        for (int ti = 0; ti < mesh.getStoredTriangleNum(); ti++) {
            if (mesh.isTriangleValid(ti)) {
                int p0Index = mesh.getTrianglePointIndex(ti, 0);
                int p1Index = mesh.getTrianglePointIndex(ti, 1);
                int p2Index = mesh.getTrianglePointIndex(ti, 2);
                
                double p0x = mesh.getPointX(p0Index);
                double p0y = mesh.getPointY(p0Index);
                double p1x = mesh.getPointX(p1Index);
                double p1y = mesh.getPointY(p1Index);
                double p2x = mesh.getPointX(p2Index);
                double p2y = mesh.getPointY(p2Index);
                
                ViewAreaRenderer.outputTriangle(
                    vertexOutput, portalOriginRelativeToCamera,
                    localXAxis, localYAxis, p0x, p0y, p1x, p1y, p2x, p2y
                );
            }
        }
    }
    
    @Override
    public boolean roughTestVisibility(
        UnilateralPortalState portalState,
        Vec3 cameraPos
    ) {
        return RectangularPortalShape.INSTANCE.roughTestVisibility(portalState, cameraPos);
    }
    
    public static SpecialFlatPortalShape createDefault() {
        return new SpecialFlatPortalShape(Mesh2D.createNewFullQuadMesh());
    }
}
