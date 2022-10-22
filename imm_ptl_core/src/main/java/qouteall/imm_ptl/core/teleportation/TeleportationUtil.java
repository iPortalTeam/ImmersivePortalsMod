package qouteall.imm_ptl.core.teleportation;

import net.minecraft.util.Tuple;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.Validate;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalState;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.my_util.DQuaternion;

import javax.annotation.Nullable;

/**
 * Calculating teleportation between a moving portal and a moving player is tricky.
 */
public class TeleportationUtil {
    public static record Teleportation(
        Portal portal,
        Vec3 lastPos, Vec3 currentPos, // these are not the last tick pos and this tick pos
        double collidingPosPortalLocalX, double collidingPosPortalLocalY,
        double tOfCollision, Vec3 collidingPos,
        PortalState collidingPortalState, // for a moving portal, it's the portal state at the time of collision
        PortalState immediateLastState, PortalState immediateCurrentState,
        Vec3 thisSidePortalPointVelocity, Vec3 otherSidePortalPointVelocity,
        double timeIntervalTicks
    ) {}
    
    private static record CollisionInfo(
        double portalLocalX, double portalLocalY,
        double tOfCollision, Vec3 collisionPos
    ) {}
    
    // check teleportation to an un-animated portal
    @Nullable
    public static Teleportation checkStaticTeleportation(
        Portal portal, Vec3 lastPos, Vec3 currentPos, double timeIntervalTicks
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
            portal,
            lastPos, currentPos,
            collisionInfo.portalLocalX, collisionInfo.portalLocalY,
            collisionInfo.tOfCollision, collisionInfo.collisionPos,
            portalState,
            portalState, portalState,
            Vec3.ZERO, Vec3.ZERO,
            timeIntervalTicks
        );
    }
    
    // check teleportation to an animated portal
    @Nullable
    public static Teleportation checkDynamicTeleportation(
        Portal portal,
        PortalState lastState, PortalState currentState,
        Vec3 lastPos, Vec3 currentPos,
        double timeIntervalTicks // used for calculating velocity
    ) {
        Vec3 lastLocalPos = lastState.getPortalLocalPos(lastPos);
        Vec3 currentLocalPos = currentState.getPortalLocalPos(currentPos);
        
        CollisionInfo collisionInfo = checkTeleportationByPortalLocalPos(portal, lastLocalPos, currentLocalPos);
        
        if (collisionInfo == null) {
            return null;
        }
        
        Tuple<Vec3, Vec3> thisSideVelocityAndOtherSideVelocityAtPoint =
            PortalState.getThisSideVelocityAndOtherSideVelocityAtPoint(
                lastState, currentState,
                collisionInfo.portalLocalX, collisionInfo.portalLocalY,
                timeIntervalTicks
            );
        
        PortalState interpolatedPortalState =
            PortalState.interpolate(lastState, currentState, collisionInfo.tOfCollision, false);
        
        return new Teleportation(
            portal,
            lastPos, currentPos,
            collisionInfo.portalLocalX, collisionInfo.portalLocalY,
            collisionInfo.tOfCollision, collisionInfo.collisionPos,
            interpolatedPortalState,
            lastState, currentState,
            thisSideVelocityAndOtherSideVelocityAtPoint.getA(),
            thisSideVelocityAndOtherSideVelocityAtPoint.getB(),
            timeIntervalTicks
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
    
    // enter the portal in lastState, exit the portal in currentState
    public static Vec3 dynamicallyTransformPoint(
        PortalState lastState, PortalState currentState,
        Vec3 pos
    ) {
        DQuaternion deltaOrientation =
            currentState.orientation.hamiltonProduct(lastState.orientation.getConjugated());
        
        Vec3 offset = pos.subtract(lastState.fromPos);
        offset = deltaOrientation.rotate(offset);
        Vec3 rotated = currentState.rotation.rotate(offset);
        Vec3 scaled = rotated.scale(currentState.scaling);
        return scaled.add(currentState.toPos);
    }
    
    public static Vec3 dynamicallyTransformVec(
        PortalState lastState, PortalState currentState,
        Vec3 vec
    ) {
        DQuaternion deltaOrientation =
            currentState.orientation.hamiltonProduct(lastState.orientation.getConjugated());
        
        Vec3 rotated = deltaOrientation.rotate(vec);
        Vec3 scaled = currentState.rotation.rotate(rotated);
        return scaled.scale(currentState.scaling);
    }
    
    public static Tuple<Vec3, Vec3> getTransformedLastTickPosAndCurrentTickPos(
        Teleportation teleportation,
        Vec3 lastTickPos, Vec3 thisTickPos
    ) {
        // There are 3 portal states:
        // 1. the last state
        // 2. the portal state at the time of collision
        // 3. the current state
        
        Vec3 collisionPointOfCurrentPortalState = teleportation.immediateCurrentState.getPointOnSurface(
            teleportation.collidingPosPortalLocalX, teleportation.collidingPosPortalLocalY
        );
        Vec3 transformedCollisionPointOfCurrentPortalState =
            teleportation.immediateCurrentState.transformPoint(collisionPointOfCurrentPortalState);
        
        Vec3 movement = thisTickPos.subtract(lastTickPos);
        Vec3 preMovement = teleportation.collidingPos.subtract(lastTickPos);
        
        Vec3 remainingMovement = thisTickPos.subtract(teleportation.collidingPos);
        
        // TODO preMovement subtract velocity
        // TODO remainingMovement add velocity
        
        Vec3 transformedPreMovement = dynamicallyTransformVec(
            teleportation.collidingPortalState, teleportation.immediateCurrentState, preMovement
        );
        Vec3 transformedRemainingMovement = dynamicallyTransformVec(
            teleportation.collidingPortalState, teleportation.immediateCurrentState, remainingMovement
        );
        
        return new Tuple<>(
            transformedCollisionPointOfCurrentPortalState.subtract(transformedPreMovement),
            transformedCollisionPointOfCurrentPortalState.add(transformedRemainingMovement)
        );
    }
    
}
