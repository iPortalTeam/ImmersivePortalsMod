package qouteall.imm_ptl.core.portal.shape;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.collision.CollisionHelper;
import qouteall.imm_ptl.core.collision.PortalCollisionHandler;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.animation.UnilateralPortalState;
import qouteall.imm_ptl.core.render.ViewAreaRenderer;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.my_util.BoxPredicateF;
import qouteall.q_misc_util.my_util.IntBox;
import qouteall.q_misc_util.my_util.Plane;
import qouteall.q_misc_util.my_util.Range;
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
    public AABB getBoundingBox(
        UnilateralPortalState portalState, boolean limitSize,
        double boxExpand
    ) {
        double halfW = portalState.width() / 2 + boxExpand;
        double halfH = portalState.height() / 2 + boxExpand;
        double halfT = portalState.thickness() / 2 + boxExpand;
        
        if (limitSize) {
            halfW = Math.min(halfW, 32);
            halfH = Math.min(halfH, 32);
            halfT = Math.min(halfT, 32);
        }
        
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
    public double roughDistanceToPortalShape(UnilateralPortalState portalState, Vec3 pos) {
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
        UnilateralPortalState thisSideState, UnilateralPortalState otherSideState,
        Portal portal
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
            return !in;
        }
        else {
            return in;
        }
    }
    
    @Environment(EnvType.CLIENT)
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
    
    @Override
    public boolean canCollideWith(
        Portal portal, UnilateralPortalState portalState,
        Vec3 entityEyePos, AABB entityBoundingBox
    ) {
        AABB expandedBox = getBoundingBox(
            portalState, false, 2.0
        );
        return expandedBox.intersects(entityBoundingBox);
    }
    
    @Override
    public boolean isLocalBoxInPortalProjection(
        UnilateralPortalState portalState,
        double minX, double minY, double minZ, double maxX, double maxY, double maxZ
    ) {
        return Range.rangeIntersects(
            -portalState.width() / 2, portalState.width() / 2, minX, maxX
        ) && Range.rangeIntersects(
            -portalState.height() / 2, portalState.height() / 2, minY, maxY
        ) && Range.rangeIntersects(
            -portalState.thickness() / 2, portalState.thickness() / 2, minZ, maxZ
        );
    }
    
    @Override
    public Vec3 getMovementForPushingEntityOutOfPortal(
        Portal portal, UnilateralPortalState portalState, Entity entity,
        Vec3 attemptedMove
    ) {
        AABB entityLocalBox = Helper.transformBox(
            entity.getBoundingBox(), portalState::transformGlobalToLocal
        );
        
        Vec3 localMove = portalState.transformVecGlobalToLocal(attemptedMove);
        
        AABB movedEntityLocalBox = entityLocalBox.move(localMove);
        
        AABB portalLocalBox = new AABB(
            -portalState.width(), -portalState.height(), -portalState.thickness(),
            portalState.width(), portalState.height(), portalState.thickness()
        );
        Vec3 offset = facingOutwards ?
            PortalCollisionHandler.getOffsetForPushingBoxOutOfAABB(
                movedEntityLocalBox, portalLocalBox
            ) :
            PortalCollisionHandler.getOffsetForConfiningBoxInsideAABB(
                movedEntityLocalBox, portalLocalBox
            );
        
        return portalState.transformVecLocalToGlobal(localMove.add(offset));
    }
    
    @Override
    public PortalShape cloneIfNecessary() {
        // the object does not contain any mutable field
        // no need to clone
        return this;
    }
    
    @Override
    public @Nullable AABB transformEntityActiveCollisionBox(Portal portal, AABB box, Entity entity) {
        Vec3 eyePos = entity.getEyePosition(1);
        UnilateralPortalState thisSideState = portal.getThisSideState();
        Vec3 localEyePos = thisSideState.transformGlobalToLocal(eyePos);
        
        AABB currBox = box;
        
        if (currBox != null && localEyePos.x() < -thisSideState.width() / 2) {
            currBox = CollisionHelper.clipBox(
                currBox,
                thisSideState.transformLocalToGlobal(new Vec3(-thisSideState.width() / 2, 0, 0)),
                thisSideState.transformVecLocalToGlobal(new Vec3(-1, 0, 0))
            );
        }
        
        if (currBox != null && localEyePos.x() > thisSideState.width() / 2) {
            currBox = CollisionHelper.clipBox(
                currBox,
                thisSideState.transformLocalToGlobal(new Vec3(thisSideState.width() / 2, 0, 0)),
                thisSideState.transformVecLocalToGlobal(new Vec3(1, 0, 0))
            );
        }
        
        if (currBox != null && localEyePos.y() < -thisSideState.height() / 2) {
            currBox = CollisionHelper.clipBox(
                currBox,
                thisSideState.transformLocalToGlobal(new Vec3(0, -thisSideState.height() / 2, 0)),
                thisSideState.transformVecLocalToGlobal(new Vec3(0, -1, 0))
            );
        }
        
        if (currBox != null && localEyePos.y() > thisSideState.height() / 2) {
            currBox = CollisionHelper.clipBox(
                currBox,
                thisSideState.transformLocalToGlobal(new Vec3(0, thisSideState.height() / 2, 0)),
                thisSideState.transformVecLocalToGlobal(new Vec3(0, 1, 0))
            );
        }
        
        if (currBox != null && localEyePos.z() < -thisSideState.thickness() / 2) {
            currBox = CollisionHelper.clipBox(
                currBox,
                thisSideState.transformLocalToGlobal(new Vec3(0, 0, -thisSideState.thickness() / 2)),
                thisSideState.transformVecLocalToGlobal(new Vec3(0, 0, -1))
            );
        }
        
        if (currBox != null && localEyePos.z() > thisSideState.thickness() / 2) {
            currBox = CollisionHelper.clipBox(
                currBox,
                thisSideState.transformLocalToGlobal(new Vec3(0, 0, thisSideState.thickness() / 2)),
                thisSideState.transformVecLocalToGlobal(new Vec3(0, 0, 1))
            );
        }
        
        return currBox;
    }
    
    @Override
    public @Nullable SectionPos getModifiedVisibleSectionIterationOrigin(
        Portal portal, Vec3 innerCameraPos
    ) {
        if (!IPGlobal.boxPortalSpecialIteration) {
            return null;
        }
        
        if (!facingOutwards) {
            return null;
        }
        
        InnerSectionRange r = getInnerSectionRange(portal);
        SectionPos cameraPosSection = SectionPos.of(innerCameraPos);
        
        int secX = Mth.clamp(cameraPosSection.x(), r.l().x(), r.hInclusive().x());
        int secY = Mth.clamp(cameraPosSection.y(), r.l().y(), r.hInclusive().y());
        int secZ = Mth.clamp(cameraPosSection.z(), r.l().z(), r.hInclusive().z());
        
        return SectionPos.of(secX, secY, secZ);
    }
    
    private InnerSectionRange getInnerSectionRange(Portal portal) {
        AABB otherSideBoundingBox = getReverse().getBoundingBox(
            portal.getOtherSideState(), false, 0
        );
        
        IntBox otherSideIntBox = IntBox.fromRealNumberBox(otherSideBoundingBox);
        
        SectionPos lSectionPos = SectionPos.of(otherSideIntBox.l);
        SectionPos hSectionPos = SectionPos.of(otherSideIntBox.h);
        return new InnerSectionRange(lSectionPos, hSectionPos);
    }
    
    private record InnerSectionRange(
        SectionPos l, SectionPos hInclusive
    ) {}
    
    @Override
    public @Nullable BoxPredicateF getInnerFrustumCullingFunc(Portal portal, Vec3 cameraPos) {
        if (!IPGlobal.boxPortalSpecialIteration) {
            return null;
        }
        
        if (!facingOutwards) {
            return null;
        }
        
        InnerSectionRange innerSectionRange = getInnerSectionRange(portal);
        
        float lx = (float) (innerSectionRange.l.x() * 16 - cameraPos.x);
        float ly = (float) (innerSectionRange.l.y() * 16 - cameraPos.y);
        float lz = (float) (innerSectionRange.l.z() * 16 - cameraPos.z);
        float hx = (float) ((innerSectionRange.hInclusive.x() + 1) * 16 - cameraPos.x);
        float hy = (float) ((innerSectionRange.hInclusive.y() + 1) * 16 - cameraPos.y);
        float hz = (float) ((innerSectionRange.hInclusive.z() + 1) * 16 - cameraPos.z);
        
        return (minX, minY, minZ, maxX, maxY, maxZ) -> {
            float midX = (minX + maxX) / 2;
            float midY = (minY + maxY) / 2;
            float midZ = (minZ + maxZ) / 2;
            
            return !(midX > lx && midX < hx &&
                midY > ly && midY < hy &&
                midZ > lz && midZ < hz);
        };
    }
}
