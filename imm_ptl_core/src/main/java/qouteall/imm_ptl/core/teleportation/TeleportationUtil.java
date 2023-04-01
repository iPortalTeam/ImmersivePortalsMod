package qouteall.imm_ptl.core.teleportation;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.Validate;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalState;
import qouteall.q_misc_util.Helper;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;

/**
 * Calculating teleportation between a moving portal and a moving player is tricky.
 */
public class TeleportationUtil {
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
        double localX,
        double localY
    ) {
        Vec3 lastThisSidePos = lastTickState.getPointOnSurface(localX, localY);
        Vec3 currentThisSidePos = thisTickState.getPointOnSurface(localX, localY);
        Vec3 thisSideVelocity = currentThisSidePos.subtract(lastThisSidePos);
        
        Vec3 lastOtherSidePos = lastTickState.transformPoint(lastThisSidePos);
        Vec3 currentOtherSidePos = thisTickState.transformPoint(currentThisSidePos);
        Vec3 otherSideVelocity = currentOtherSidePos.subtract(lastOtherSidePos);
        
        return new PortalPointVelocity(thisSideVelocity, otherSideVelocity);
    }
    
    public static void transformEntityVelocity(
        Portal portal, Entity entity,
        PortalPointVelocity portalPointVelocity
    ) {
        Vec3 oldVelocityRelativeToPortal = McHelper.getWorldVelocity(entity).subtract(portalPointVelocity.thisSidePointVelocity());
        Vec3 transformedVelocityRelativeToPortal = portal.transformVelocityRelativeToPortal(oldVelocityRelativeToPortal, entity);
        Vec3 newVelocity = transformedVelocityRelativeToPortal.add(portalPointVelocity.otherSidePointVelocity());
        McHelper.setWorldVelocity(entity, newVelocity);
    }
    
    public static record Teleportation(
        boolean isDynamic,
        Portal portal,
        Vec3 lastFrameEyePos, Vec3 thisFrameEyePos,
        double collidingPosPortalLocalX, double collidingPosPortalLocalY,
        double tOfCollision, Vec3 collidingPos,
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
        public static final PortalPointVelocity zero = new PortalPointVelocity(Vec3.ZERO, Vec3.ZERO);
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
        
        CollisionInfo collisionInfo = checkTeleportationByPortalLocalPos(
            portal, lastLocalPos, currentLocalPos
        );
        
        if (collisionInfo == null) {
            return null;
        }
        
        Vec3 newLastTickEyePos = portal.transformPoint(lastTickEyePos);
        Vec3 newThisTickEyePos = portal.transformPoint(thisTickEyePos);
        
        PortalState portalState = portal.getPortalState();
        return new Teleportation(
            false,
            portal,
            lastPos, currentPos,
            collisionInfo.portalLocalX, collisionInfo.portalLocalY,
            collisionInfo.tOfCollision, collisionInfo.collisionPos,
            portalState,
            portalState, portalState,
            portalState, portalState,
            PortalPointVelocity.zero,
            portal.transformPoint(collisionInfo.collisionPos),
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
        
        CollisionInfo collisionInfo = checkTeleportationByPortalLocalPos(
            portal, lastLocalPos, currentLocalPos
        );
        
        if (collisionInfo == null) {
            return null;
        }
        
        double collisionLocalX = collisionInfo.portalLocalX;
        double collisionLocalY = collisionInfo.portalLocalY;
        PortalPointVelocity portalPointVelocity = getPortalPointVelocity(
            lastTickState, thisTickState,
            collisionLocalX, collisionLocalY
        );
        
        PortalState collisionPortalState =
            PortalState.interpolate(lastFrameState, currentFrameState, collisionInfo.tOfCollision, false);
        Vec3 collisionPointMappedToThisFrame =
            thisTickState.getLocalPosTransformed(collisionLocalX, collisionLocalY);
        Vec3 collisionPointMappedToLastFrame =
            lastFrameState.getLocalPosTransformed(collisionLocalX, collisionLocalY);
//        Vec3 teleportationCheckpoint = Helper.maxBy(
//            collisionPointMappedToThisFrame, collisionPointMappedToLastFrame,
//            Comparator.comparingDouble(
//                v -> v.subtract(portal.getDestPos()).dot(portal.getContentDirection())
//            )
//        );
        
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
        
        {
            // make sure that the end-tick pos is in portal destination side
            double dot = newOtherSideThisTickPos
                .subtract(thisTickState.toPos)
                .dot(thisTickState.getContentDirection());
            if (dot < 0) {
                Helper.log("Teleported to behind the end-tick portal destination. Corrected.");
                newOtherSideThisTickPos = newOtherSideThisTickPos.add(
                    thisTickState.getContentDirection().scale(-dot + 0.001)
                );
            }
        }
        
        {
            // make sure that the end-frame camera pos in portal destination side
            newImmediateCameraPos = newOtherSideLastTickPos.lerp(newOtherSideThisTickPos, partialTicks);
            
            double dot = newImmediateCameraPos
                .subtract(currentFrameState.toPos)
                .dot(currentFrameState.getContentDirection());
            if (dot < 0) {
                Helper.log("Teleported to behind the end-frame portal destination. Corrected.");
                Vec3 offset1 = currentFrameState.getContentDirection().scale(-dot + 0.001);
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
            if (dot < 0) {
                Helper.log("Teleported to behind the last-frame portal destination. Corrected.");
                Vec3 offset1 = lastFrameState.getContentDirection().scale(-dot + 0.001);
                newOtherSideThisTickPos = newOtherSideThisTickPos.add(offset1);
                newOtherSideLastTickPos = newOtherSideLastTickPos.add(offset1);
            }
        }
        
        Vec3 teleportationCheckpoint = newOtherSideLastTickPos.lerp(newOtherSideThisTickPos, partialTicks);
        
        return new Teleportation(
            true,
            portal,
            lastFrameEyePos, currentFrameEyePos,
            collisionLocalX, collisionLocalY,
            collisionInfo.tOfCollision, collisionInfo.collisionPos,
            collisionPortalState,
            lastFrameState, currentFrameState,
            lastTickState, thisTickState,
            portalPointVelocity,
            teleportationCheckpoint,
            newOtherSideLastTickPos, newOtherSideThisTickPos
        );
    }
    
    // use the portal-local coordinate to simplify teleportation check
    @Nullable
    private static CollisionInfo checkTeleportationByPortalLocalPos(
        Portal portal, Vec3 lastLocalPos, Vec3 currentLocalPos
    ) {
        boolean movedThrough = lastLocalPos.z > 0 && currentLocalPos.z < 0;
        
        if (!movedThrough) {
            return null;
        }
        
        Vec3 lineOrigin = lastLocalPos;
        Vec3 lineDirection = currentLocalPos.subtract(lastLocalPos);
        
        double t = Helper.getCollidingT(
            Vec3.ZERO, new Vec3(0, 0, 1), lineOrigin, lineDirection
        );
        Validate.isTrue(t < 1.00001 && t > -0.00001);
        Vec3 collidingPoint = lineOrigin.add(lineDirection.scale(t));
        
        boolean inProjection = portal.isLocalXYOnPortal(collidingPoint.x, collidingPoint.y);
        
        if (inProjection) {
            return new CollisionInfo(
                collidingPoint.x, collidingPoint.y,
                t, portal.transformFromPortalLocalToWorld(collidingPoint)
            );
        }
        else {
            return null;
        }
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
                localPos.x, localPos.y
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
