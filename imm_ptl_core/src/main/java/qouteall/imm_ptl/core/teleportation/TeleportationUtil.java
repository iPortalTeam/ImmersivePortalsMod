package qouteall.imm_ptl.core.teleportation;

import net.minecraft.util.Tuple;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.Validate;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalState;
import qouteall.imm_ptl.core.render.context_management.RenderStates;
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
        Vec3 teleportationCheckpoint
    ) {}
    
    public static record PortalPointVelocity(
        Vec3 thisSidePointVelocity,
        Vec3 otherSidePointVelocity
    ) {
    
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
        Portal portal, Vec3 lastPos, Vec3 currentPos
    ) {
        Vec3 lastLocalPos = portal.transformFromWorldToPortalLocal(lastPos);
        Vec3 currentLocalPos = portal.transformFromWorldToPortalLocal(currentPos);
        
        CollisionInfo collisionInfo = checkTeleportationByPortalLocalPos(
            portal, lastLocalPos, currentLocalPos
        );
        
        if (collisionInfo == null) {
            return null;
        }
        
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
            new PortalPointVelocity(Vec3.ZERO, Vec3.ZERO),
            portal.transformPoint(collisionInfo.collisionPos)
        );
    }
    
    // check teleportation to an animated portal
    @Nullable
    public static Teleportation checkDynamicTeleportation(
        Portal portal,
        PortalState lastFrameState, PortalState currentFrameState,
        Vec3 lastFrameEyePos, Vec3 currentFrameEyePos,
        PortalState lastTickState, PortalState thisTickState,
        Vec3 lastTickEyePos, Vec3 thisTickEyePos
    ) {
        Vec3 lastLocalPos = lastFrameState.worldPosToPortalLocalPos(lastFrameEyePos);
        Vec3 currentLocalPos = currentFrameState.worldPosToPortalLocalPos(currentFrameEyePos);
        
        CollisionInfo collisionInfo = checkTeleportationByPortalLocalPos(portal, lastLocalPos, currentLocalPos);
        
        if (collisionInfo == null) {
            return null;
        }
        
        PortalPointVelocity portalPointVelocity = getPortalPointVelocity(
            lastTickState, thisTickState,
            collisionInfo.portalLocalX, collisionInfo.portalLocalY
        );
//        PortalPointVelocity portalPointVelocity = getConservativePortalPointVelocity(
//            lastTickState, thisTickState, lastTickEyePos, thisTickEyePos
//        );
        
        PortalState collisionPortalState =
            PortalState.interpolate(lastFrameState, currentFrameState, collisionInfo.tOfCollision, false);
        Vec3 collisionPointMappedToThisFrame = thisTickState.transformPoint(thisTickState.portalLocalPosToWorldPos(new Vec3(
            collisionInfo.portalLocalX, collisionInfo.portalLocalY, 0
        )));
        Vec3 collisionPointMappedToLastFrame = lastFrameState.transformPoint(lastFrameState.portalLocalPosToWorldPos(new Vec3(
            collisionInfo.portalLocalX, collisionInfo.portalLocalY, 0
        )));
        Vec3 teleportationCheckpoint = Helper.maxBy(
            collisionPointMappedToThisFrame, collisionPointMappedToLastFrame,
            Comparator.comparingDouble(
                v -> v.subtract(portal.getDestPos()).dot(portal.getContentDirection())
            )
        );
        
        return new Teleportation(
            true,
            portal,
            lastFrameEyePos, currentFrameEyePos,
            collisionInfo.portalLocalX, collisionInfo.portalLocalY,
            collisionInfo.tOfCollision, collisionInfo.collisionPos,
            collisionPortalState,
            lastFrameState, currentFrameState,
            lastTickState, thisTickState,
            portalPointVelocity,
            teleportationCheckpoint
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
    
    public static Tuple<Vec3, Vec3> getTransformedLastTickPosAndCurrentTickPos(
        Teleportation teleportation,
        Vec3 lastTickPos, Vec3 thisTickPos
    ) {
        if (!teleportation.isDynamic()) {
            // simple static teleportation
            Portal portal = teleportation.portal;
            Vec3 newLastTickPos = portal.transformPoint(lastTickPos);
            Vec3 newThisTickPos = portal.transformPoint(thisTickPos);
            return new Tuple<>(newLastTickPos, newThisTickPos);
        }
        
        // dynamic teleportation
        
        PortalState lastTickState = teleportation.lastTickState;
        PortalState thisTickState = teleportation.thisTickState;
        
        Vec3 newOtherSideLastTickPos = lastTickState.transformPoint(lastTickPos);
        Vec3 newOtherSideThisTickPos = thisTickState.transformPoint(thisTickPos);
        
        float partialTicks = RenderStates.tickDelta;
        
        Vec3 newImmediateCameraPos = newOtherSideLastTickPos.lerp(newOtherSideThisTickPos, partialTicks);

        Vec3 correctImmediateCameraPos =
            teleportation.collidingPortalState().transformPoint(teleportation.thisFrameEyePos());
        
        Vec3 deltaVelocity = teleportation.portalPointVelocity().otherSidePointVelocity().subtract(
            teleportation.collidingPortalState().transformVec(teleportation.portalPointVelocity().thisSidePointVelocity())
        );
        
        Vec3 offset = correctImmediateCameraPos.subtract(newImmediateCameraPos);
        
        newOtherSideLastTickPos = newOtherSideLastTickPos.add(offset);
        newOtherSideThisTickPos = newOtherSideThisTickPos.add(offset);
        
        {
            PortalState targetingPortalState = teleportation.thisTickState();
            double dot = newOtherSideThisTickPos
                .subtract(targetingPortalState.toPos)
                .dot(targetingPortalState.getContentDirection());
            if (dot < 0) {
                Helper.log("Teleported to behind the end-tick portal destination. Corrected.");
                newOtherSideThisTickPos = newOtherSideThisTickPos.add(
                    targetingPortalState.getContentDirection().scale(-dot + 0.001)
                );
            }
        }
        
        {
            PortalState thisFrameState = teleportation.thisFrameState();
            newImmediateCameraPos = newOtherSideLastTickPos.lerp(newOtherSideThisTickPos, partialTicks);
            
            double dot = newImmediateCameraPos
                .subtract(thisFrameState.toPos)
                .dot(thisFrameState.getContentDirection());
            if (dot < 0) {
                Helper.log("Teleported to behind the end-frame portal destination. Corrected.");
                Vec3 offset1 = thisFrameState.getContentDirection().scale(-dot + 0.001);
                newOtherSideThisTickPos = newOtherSideThisTickPos.add(offset1);
                newOtherSideLastTickPos = newOtherSideLastTickPos.add(offset1);
            }
        }
        
        return new Tuple<>(newOtherSideLastTickPos, newOtherSideThisTickPos);
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
