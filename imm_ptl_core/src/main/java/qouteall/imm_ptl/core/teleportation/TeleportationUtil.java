package qouteall.imm_ptl.core.teleportation;

import net.minecraft.util.Tuple;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.Validate;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalState;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.my_util.DQuaternion;

import javax.annotation.Nullable;
import java.util.Comparator;

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
    
    public static record PortalPointVelocity(
        Vec3 thisSidePointVelocity,
        Vec3 otherSidePointVelocity
    ){
    
    }
    
    public static record PortalPointOffset(
        Vec3 thisSideOffset,
        Vec3 otherSideOffse
    ){}
    
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
//        DQuaternion deltaOrientation =
//            lastState.orientation.getConjugated().hamiltonProduct(currentState.orientation);
        DQuaternion deltaOrientation =
            currentState.orientation.hamiltonProduct(lastState.orientation.getConjugated());
        
        Vec3 rotated = deltaOrientation.rotate(vec);
//        Vec3 rotated = vec;
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
        Vec3 transformedCollisionPointOfCollisionPortalState =
            teleportation.collidingPortalState().transformPoint(collisionPointOfCurrentPortalState);
    
        Vec3 movement = thisTickPos.subtract(lastTickPos);
        Vec3 preMovement = teleportation.collidingPos.subtract(lastTickPos);

        // NOTE the partial tick of collision is smaller than the current partial tick
        double partialTickOfCollision =
            movement.length() < 0.000001 ? 0.5 : preMovement.length() / movement.length();

        Vec3 transformedMovement = dynamicallyTransformVec(
            teleportation.collidingPortalState, teleportation.immediateCurrentState, movement
        );

        transformedMovement = transformedMovement.add(
            teleportation.otherSidePortalPointVelocity.scale(teleportation.timeIntervalTicks)
        );

        return new Tuple<>(
            transformedCollisionPointOfCollisionPortalState.subtract(transformedMovement.scale(partialTickOfCollision)),
            transformedCollisionPointOfCollisionPortalState.add(transformedMovement.scale(1 - partialTickOfCollision))
        );
        
//        Vec3 movement = thisTickPos.subtract(lastTickPos);
//        Vec3 preMovement = teleportation.collidingPos.subtract(lastTickPos);
//
//        Vec3 remainingMovement = thisTickPos.subtract(teleportation.collidingPos);
//
//        Vec3 transformedPreMovement = dynamicallyTransformVec(
//            teleportation.collidingPortalState, teleportation.immediateCurrentState, preMovement
//        );
//        Vec3 transformedRemainingMovement = dynamicallyTransformVec(
//            teleportation.collidingPortalState, teleportation.immediateCurrentState, remainingMovement
//        );
//
//        return new Tuple<>(
//            transformedCollisionPointOfCollisionPortalState.subtract(transformedPreMovement),
//            transformedCollisionPointOfCollisionPortalState.add(transformedRemainingMovement)
//        );
    }
    
    private static PortalPointVelocity getConservativePortalPointVelocity(
        Portal portal, PortalState lastState, PortalState currentState,
        double timeIntervalTicks, Vec3 currentLocalPos,
        CollisionInfo collisionInfo
    ) {
        PortalPointVelocity relativeVelocityAtCollisionPoint =
            getThisSideVelocityAndOtherSideVelocityAtPoint(
                lastState, currentState,
                collisionInfo.portalLocalX, collisionInfo.portalLocalY,
                timeIntervalTicks
            );
        
        PortalPointVelocity relativeVelocityAtCurrentTickPoint =
            getThisSideVelocityAndOtherSideVelocityAtPoint(
                lastState, currentState,
                currentLocalPos.x, currentLocalPos.y,
                timeIntervalTicks
            );
        
        Vec3 thisSidePortalPointVelocity = Helper.minBy(
            relativeVelocityAtCollisionPoint.thisSidePointVelocity(),
            relativeVelocityAtCurrentTickPoint.thisSidePointVelocity(),
            Comparator.comparingDouble((p) -> p.dot(portal.getNormal()))
        );
        
        Vec3 otherSidePortalPointVelocity = Helper.maxBy(
            relativeVelocityAtCollisionPoint.otherSidePointVelocity(),
            relativeVelocityAtCurrentTickPoint.otherSidePointVelocity(),
            Comparator.comparingDouble((p) -> p.dot(portal.getContentDirection()))
        );
        
        return new PortalPointVelocity(
            thisSidePortalPointVelocity,
            otherSidePortalPointVelocity
        );
    }
    
    /**
     * We have to carefully calculate the relative velocity between a moving player and a moving portal.
     * The velocity in each point is different if the portal is rotating.
     *
     * The velocity is calculated from the two states of the portal assuming the portal moves linearly.
     * However, this will be wrong when the portal is rotating.
     */
    public static PortalPointVelocity getThisSideVelocityAndOtherSideVelocityAtPoint(
        PortalState lastState,
        PortalState currentState,
        double localX,
        double localY,
        double timeInterval
    ) {
        Validate.isTrue(timeInterval > 0);
        
        Vec3 lastThisSidePos = lastState.getPointOnSurface(localX, localY);
        Vec3 currentThisSidePos = currentState.getPointOnSurface(localX, localY);
        Vec3 thisSideVelocity = currentThisSidePos.subtract(lastThisSidePos).scale(1.0 / timeInterval);
        
        Vec3 lastOtherSidePos = lastState.transformPoint(lastThisSidePos);
        Vec3 currentOtherSidePos = currentState.transformPoint(currentThisSidePos);
        Vec3 otherSideVelocity = currentOtherSidePos.subtract(lastOtherSidePos).scale(1.0 / timeInterval);
        
        return new PortalPointVelocity(thisSideVelocity, otherSideVelocity);
    }
    
}
