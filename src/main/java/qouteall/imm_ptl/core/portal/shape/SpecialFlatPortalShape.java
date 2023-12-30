package qouteall.imm_ptl.core.portal.shape;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.animation.UnilateralPortalState;
import qouteall.imm_ptl.core.render.ViewAreaRenderer;
import qouteall.q_misc_util.my_util.BoxPredicateF;
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
    public AABB getBoundingBox(
        UnilateralPortalState portalState, boolean limitSize, double boxExpand
    ) {
        return RectangularPortalShape.INSTANCE.getBoundingBox(
            portalState, limitSize, boxExpand
        );
    }
    
    // it does not calculate "real" distance here
    @Override
    public double roughDistanceToPortalShape(UnilateralPortalState portalState, Vec3 pos) {
        return RectangularPortalShape.INSTANCE.roughDistanceToPortalShape(portalState, pos);
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
        UnilateralPortalState thisSideState, UnilateralPortalState otherSideState,
        Portal portal
    ) {
        return RectangularPortalShape.INSTANCE.getInnerClipping(
            thisSideState, otherSideState, portal
        );
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
    
    @Environment(EnvType.CLIENT)
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
    public boolean canCollideWith(
        Portal portal, UnilateralPortalState portalState, Vec3 entityEyePos, AABB entityBoundingBox
    ) {
        boolean inFrontOfPortal = portal.isInFrontOfPortal(entityEyePos);
        
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
        boolean roughTest = RectangularPortalShape.INSTANCE.isLocalBoxInPortalProjection(
            portalState, minX, minY, minZ, maxX, maxY, maxZ
        );
        
        if (!roughTest) {
            return false;
        }
        
        double halfWidth = portalState.width() / 2;
        double halfHeight = portalState.height() / 2;
        
        return mesh.boxIntersects(
            minX / halfWidth, minY / halfHeight,
            maxX / halfWidth, maxY / halfHeight
        );
    }
    
    @Override
    public Vec3 getMovementForPushingEntityOutOfPortal(Portal portal, UnilateralPortalState portalState, Entity entity, Vec3 attemptedMove) {
        return RectangularPortalShape.INSTANCE.getMovementForPushingEntityOutOfPortal(
            portal, portalState, entity, attemptedMove
        );
    }
    
    @Override
    public PortalShape cloneIfNecessary() {
        return new SpecialFlatPortalShape(mesh.copy());
    }
    
    @Override
    public boolean roughTestVisibility(
        UnilateralPortalState portalState,
        Vec3 cameraPos,
        boolean isIrisShaderOn
    ) {
        return RectangularPortalShape.INSTANCE.roughTestVisibility(
            portalState, cameraPos, isIrisShaderOn
        );
    }
    
    public static SpecialFlatPortalShape createDefault() {
        return new SpecialFlatPortalShape(Mesh2D.createNewFullQuadMesh());
    }
    
    @Environment(EnvType.CLIENT)
    @Override
    public BoxPredicateF getInnerFrustumCullingFunc(
        Portal portal,
        Vec3 cameraPos
    ) {
        return RectangularPortalShape.INSTANCE.getInnerFrustumCullingFunc(
            portal, cameraPos
        );
    }
    
    @Environment(EnvType.CLIENT)
    @Override
    public BoxPredicateF getOuterFrustumCullingFunc(
        Portal portal,
        Vec3 cameraPos
    ) {
        return RectangularPortalShape.INSTANCE.getOuterFrustumCullingFunc(
            portal, cameraPos
        );
    }
    
    @Override
    public VoxelShape getThisSideCollisionExclusion(UnilateralPortalState portalState) {
        return RectangularPortalShape.INSTANCE.getThisSideCollisionExclusion(portalState);
    }
    
    @Override
    public @Nullable AABB transformEntityActiveCollisionBox(Portal portal, AABB box, Entity entity) {
        return RectangularPortalShape.INSTANCE.transformEntityActiveCollisionBox(portal, box, entity);
    }
}
