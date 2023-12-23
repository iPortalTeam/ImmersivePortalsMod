package qouteall.imm_ptl.core.teleportation;

import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalState;
import qouteall.imm_ptl.core.portal.animation.UnilateralPortalState;
import qouteall.imm_ptl.core.portal.shape.PortalShape;
import qouteall.q_misc_util.my_util.RayTraceResult;

import java.util.Comparator;
import java.util.List;

/**
 * Calculating teleportation between a moving portal and a moving player is tricky.
 */
public class TeleportationUtil {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    /**
     * We have to carefully calculate the relative velocity between a moving player and a moving portal.
     * The velocity in each point is different if the portal is rotating.
     * <p>
     * The velocity is calculated from the two states of the portal assuming the portal moves linearly.
     * However, this will be wrong when the portal is rotating.
     */
    public static PortalPointVelocity getPortalPointVelocity(
        PortalState lastTickState,
        PortalState thisTickState,
        Vec3 localPos
    ) {
        Vec3 lastThisSidePos = lastTickState.getThisSideState()
            .transformLocalToGlobal(localPos);
        Vec3 currentThisSidePos = thisTickState.getThisSideState()
            .transformLocalToGlobal(localPos);
        
        Vec3 thisSideVelocity = currentThisSidePos.subtract(lastThisSidePos);
        
        Vec3 lastOtherSidePos = lastTickState.transformPoint(lastThisSidePos);
        Vec3 currentOtherSidePos = thisTickState.transformPoint(currentThisSidePos);
        Vec3 otherSideVelocity = currentOtherSidePos.subtract(lastOtherSidePos);
        
        return new PortalPointVelocity(thisSideVelocity, otherSideVelocity);
    }
    
    public static void transformEntityVelocity(
        Portal portal, Entity entity,
        PortalPointVelocity portalPointVelocity,
        Vec3 oldEntityPos
    ) {
        Vec3 oldVelocityRelativeToPortal = McHelper.getWorldVelocity(entity).subtract(portalPointVelocity.thisSidePointVelocity());
        Vec3 transformedVelocityRelativeToPortal = portal.transformVelocityRelativeToPortal(oldVelocityRelativeToPortal, entity, oldEntityPos);
        Vec3 newVelocity = transformedVelocityRelativeToPortal.add(portalPointVelocity.otherSidePointVelocity());
        McHelper.setWorldVelocity(entity, newVelocity);
    }
    
    public static record Teleportation(
        boolean isDynamic,
        Portal portal,
        Vec3 lastWorldEyePos, Vec3 currentWorldEyePos,
        Vec3 lastLocalEyePos, Vec3 currentLocalEyePos,
        Vec3 localCollisionPoint,
        double tOfCollision,
        Vec3 worldCollisionPoint,
        Vec3 worldSurfaceNormal,
        PortalState collidingPortalState, // for a moving portal, it's the portal state at the time of collision
        PortalState lastFrameState, PortalState thisFrameState,
        PortalState lastTickState, PortalState thisTickState,
        PortalPointVelocity portalPointVelocity,
        Vec3 teleportationCheckpoint,
        Vec3 newLastTickEyePos, Vec3 newThisTickEyePos
    ) {}
    
    public static record PortalPointVelocity(
        Vec3 thisSidePointVelocity,
        Vec3 otherSidePointVelocity
    ) {
        public static final PortalPointVelocity ZERO = new PortalPointVelocity(Vec3.ZERO, Vec3.ZERO);
    }
    
    public static record PortalPointOffset(
        Vec3 thisSideOffset,
        Vec3 otherSideOffse
    ) {}
    
    private static record CollisionInfo(
        double portalLocalX, double portalLocalY,
        double tOfCollision, Vec3 collisionPos
    ) {}
    
    // check teleportation to an un-animated portal
    @Nullable
    public static Teleportation checkStaticTeleportation(
        Portal portal, Vec3 lastPos, Vec3 currentPos,
        Vec3 lastTickEyePos, Vec3 thisTickEyePos
    ) {
        Vec3 lastLocalPos = portal.transformFromWorldToPortalLocal(lastPos);
        Vec3 currentLocalPos = portal.transformFromWorldToPortalLocal(currentPos);
        
        PortalShape portalShape = portal.getPortalShape();
        UnilateralPortalState portalThisSideState = portal.getThisSideState();
        
        // use portal-local coordinate to simplify teleportation check
        RayTraceResult localRayTraceResult = portalShape.raytracePortalShapeByLocalPos(
            portalThisSideState,
            lastLocalPos, currentLocalPos,
            0
        );
        
        if (localRayTraceResult == null) {
            return null;
        }
        
        Vec3 worldHitPos = portalThisSideState.transformLocalToGlobal(
            localRayTraceResult.hitPos()
        );
        
        Vec3 newLastTickEyePos = portal.transformPoint(lastTickEyePos);
        Vec3 newThisTickEyePos = portal.transformPoint(thisTickEyePos);
        
        PortalState portalState = portal.getPortalState();
        return new Teleportation(
            false,
            portal,
            lastPos, currentPos,
            lastLocalPos, currentLocalPos,
            localRayTraceResult.hitPos(),
            localRayTraceResult.t(),
            worldHitPos,
            portalThisSideState.transformVecLocalToGlobal(localRayTraceResult.surfaceNormal()),
            portalState,
            portalState, portalState,
            portalState, portalState,
            PortalPointVelocity.ZERO,
            portal.transformPoint(worldHitPos),
            newLastTickEyePos, newThisTickEyePos
        );
    }
    
    // check teleportation to an animated portal
    @Nullable
    public static Teleportation checkDynamicTeleportation(
        Portal portal,
        PortalState lastFrameState, PortalState currentFrameState,
        Vec3 lastFrameEyePos, Vec3 currentFrameEyePos,
        PortalState lastTickState, PortalState thisTickState,
        Vec3 lastTickEyePos, Vec3 thisTickEyePos, float partialTicks
    ) {
        Vec3 lastLocalPos = lastFrameState.worldPosToPortalLocalPos(lastFrameEyePos);
        Vec3 currentLocalPos = currentFrameState.worldPosToPortalLocalPos(currentFrameEyePos);
        
        UnilateralPortalState portalThisSideState = portal.getThisSideState();
        PortalShape portalShape = portal.getPortalShape();
        
        RayTraceResult rayTraceResult = portalShape.raytracePortalShapeByLocalPos(
            portalThisSideState, lastLocalPos, currentLocalPos,
            0
        );
        
        if (rayTraceResult == null) {
            return null;
        }
        
        Vec3 localHitPos = rayTraceResult.hitPos();
        Vec3 worldHitPos = portalThisSideState.transformLocalToGlobal(localHitPos);
        
        PortalPointVelocity portalPointVelocity = getPortalPointVelocity(
            lastTickState, thisTickState,
            localHitPos
        );
        
        PortalState collisionPortalState =
            PortalState.interpolate(
                lastFrameState, currentFrameState,
                rayTraceResult.t(), false
            );
        Vec3 collisionPointMappedToThisFrame =
            thisTickState.getThisSideState()
                .transformLocalToGlobal(localHitPos);
        Vec3 collisionPointMappedToLastFrame =
            lastFrameState.getThisSideState()
                .transformLocalToGlobal(localHitPos);
        
        Vec3 newOtherSideLastTickPos = lastTickState.transformPoint(lastTickEyePos);
        Vec3 newOtherSideThisTickPos = thisTickState.transformPoint(thisTickEyePos);
        
        Vec3 newImmediateCameraPos = newOtherSideLastTickPos.lerp(newOtherSideThisTickPos, partialTicks);
        
        Vec3 correctImmediateCameraPos =
            collisionPortalState.transformPoint(currentFrameEyePos);
        
        Vec3 deltaVelocity = portalPointVelocity.otherSidePointVelocity().subtract(
            collisionPortalState.transformVec(portalPointVelocity.thisSidePointVelocity())
        );
        
        Vec3 offset = correctImmediateCameraPos.subtract(newImmediateCameraPos);
        
        newOtherSideLastTickPos = newOtherSideLastTickPos.add(offset);
        newOtherSideThisTickPos = newOtherSideThisTickPos.add(offset);
        
        if (portalShape.isPlanar()) {
            {
                // make sure that the end-tick pos is in portal destination side
                double dot = newOtherSideThisTickPos
                    .subtract(thisTickState.toPos)
                    .dot(thisTickState.getContentDirection());
                if (dot < 0.00001) {
                    LOGGER.info("Teleported to behind the end-tick portal destination. Corrected.");
                    Vec3 offset1 = thisTickState.getContentDirection().scale(Math.max(-dot, 0) + 0.00001);
                    newOtherSideThisTickPos = newOtherSideThisTickPos.add(offset1);
                    newOtherSideLastTickPos = newOtherSideLastTickPos.add(offset1);
                }
            }
            
            {
                // make sure that the end-frame camera pos in portal destination side
                newImmediateCameraPos = newOtherSideLastTickPos.lerp(newOtherSideThisTickPos, partialTicks);
                
                double dot = newImmediateCameraPos
                    .subtract(currentFrameState.toPos)
                    .dot(currentFrameState.getContentDirection());
                if (dot < 0.00001) {
                    LOGGER.info("Teleported to behind the end-frame portal destination. Corrected.");
                    Vec3 offset1 = currentFrameState.getContentDirection().scale(Math.max(-dot, 0) + 0.00001);
                    newOtherSideThisTickPos = newOtherSideThisTickPos.add(offset1);
                    newOtherSideLastTickPos = newOtherSideLastTickPos.add(offset1);
                }
            }
            
            {
                // make sure that the last-frame camera pos in portal destination side
                // the portal destination may move backwards
                newImmediateCameraPos = newOtherSideLastTickPos.lerp(newOtherSideThisTickPos, partialTicks);
                
                double dot = newImmediateCameraPos
                    .subtract(lastFrameState.toPos)
                    .dot(lastFrameState.getContentDirection());
                if (dot < 0.00001) {
                    LOGGER.info("Teleported to behind the last-frame portal destination. Corrected.");
                    Vec3 offset1 = lastFrameState.getContentDirection().scale(Math.max(-dot, 0) + 0.001);
                    newOtherSideThisTickPos = newOtherSideThisTickPos.add(offset1);
                    newOtherSideLastTickPos = newOtherSideLastTickPos.add(offset1);
                }
            }
        }
        
        Vec3 teleportationCheckpoint =
            newOtherSideLastTickPos.lerp(newOtherSideThisTickPos, partialTicks);
        
        return new Teleportation(
            true,
            portal,
            lastFrameEyePos, currentFrameEyePos,
            lastLocalPos, currentLocalPos,
            localHitPos,
            rayTraceResult.t(),
            worldHitPos,
            portalThisSideState.transformVecLocalToGlobal(rayTraceResult.surfaceNormal()),
            collisionPortalState,
            lastFrameState, currentFrameState,
            lastTickState, thisTickState,
            portalPointVelocity,
            teleportationCheckpoint,
            newOtherSideLastTickPos, newOtherSideThisTickPos
        );
    }
    
    /**
     * When the portal is rotating, the outer point velocity is bigger than inner point velocity.
     * When the player teleports through portal in a tilted manner,
     * the this-side point velocity projected to normal may be smaller than the player velocity projected to normal.
     */
    @Deprecated
    private static PortalPointVelocity getConservativePortalPointVelocity(
        PortalState lastTickState, PortalState thisTickState,
        Vec3 lastTickPos, Vec3 thisTickPos
    ) {
        List<Vec3> localPoses = List.of(
            lastTickState.worldPosToPortalLocalPos(lastTickPos),
            lastTickState.worldPosToPortalLocalPos(thisTickPos),
            thisTickState.worldPosToPortalLocalPos(lastTickPos),
            thisTickState.worldPosToPortalLocalPos(thisTickPos)
        );
        
        List<PortalPointVelocity> portalPointVelocities = localPoses.stream().map(localPos ->
            getPortalPointVelocity(
                lastTickState, thisTickState,
                localPos
            )
        ).toList();
        
        return new PortalPointVelocity(
            portalPointVelocities.stream()
                .map(v -> v.thisSidePointVelocity)
                .max(Comparator.comparingDouble(
                    p -> p.dot(lastTickState.getNormal())
                ))
                .orElseThrow(),
            portalPointVelocities.stream()
                .map(v -> v.otherSidePointVelocity)
                .max(Comparator.comparingDouble(
                    p -> p.dot(thisTickState.getContentDirection())
                ))
                .orElseThrow()
        );
    }
}
