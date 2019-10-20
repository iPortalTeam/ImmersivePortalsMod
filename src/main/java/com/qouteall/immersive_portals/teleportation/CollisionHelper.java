package com.qouteall.immersive_portals.teleportation;

import com.qouteall.immersive_portals.CHelper;
import com.qouteall.immersive_portals.exposer.IEEntity;
import com.qouteall.immersive_portals.my_util.Helper;
import com.qouteall.immersive_portals.portal.Portal;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

import java.util.Comparator;
import java.util.function.Function;

public class CollisionHelper {
    
    //cut a box with a plane
    //the facing that normal points to will be remained
    //return null for empty box
    private static Box clipBox(Box box, Vec3d planePos, Vec3d planeNormal) {
        
        boolean xForward = planeNormal.x > 0;
        boolean yForward = planeNormal.y > 0;
        boolean zForward = planeNormal.z > 0;
        
        Vec3d pushedPos = new Vec3d(
            xForward ? box.minX : box.maxX,
            yForward ? box.minY : box.maxY,
            zForward ? box.minZ : box.maxZ
        );
        Vec3d staticPos = new Vec3d(
            xForward ? box.maxX : box.minX,
            yForward ? box.maxY : box.minY,
            zForward ? box.maxZ : box.minZ
        );
        
        double tOfPushedPos = Helper.getCollidingT(planePos, planeNormal, pushedPos, planeNormal);
        boolean isPushedPosInFrontOfPlane = tOfPushedPos < 0;
        if (isPushedPosInFrontOfPlane) {
            //the box is not cut by plane
            return box;
        }
        boolean isStaticPosInFrontOfPlane = Helper.isInFrontOfPlane(
            staticPos, planePos, planeNormal
        );
        if (!isStaticPosInFrontOfPlane) {
            //the box is fully cut by plane
            return null;
        }
        
        //the plane cut the box halfly
        Vec3d afterBeingPushed = pushedPos.add(planeNormal.multiply(tOfPushedPos));
        return new Box(afterBeingPushed, staticPos);
    }
    
    private static boolean shouldCollideWithPortal(Entity entity, Portal portal) {
        return portal.isTeleportable() &&
            portal.isInFrontOfPortal(entity.getCameraPosVec(1));
    }
    
    public static Vec3d handleCollisionHalfwayInPortal(
        Entity entity,
        Vec3d attemptedMove,
        Portal collidingPortal,
        Function<Vec3d, Vec3d> handleCollisionFunc
    ) {
        Box originalBoundingBox = entity.getBoundingBox();
        
        Vec3d move1 = getThisSideMove(
            entity, attemptedMove, collidingPortal,
            handleCollisionFunc, originalBoundingBox
        );
        
        Vec3d move2 = getOtherSideMove(
            entity, attemptedMove, collidingPortal,
            handleCollisionFunc, originalBoundingBox
        );
        
        return new Vec3d(
            Math.abs(move1.x) < Math.abs(move2.x) ? move1.x : move2.x,
            Math.abs(move1.y) < Math.abs(move2.y) ? move1.y : move2.y,
            Math.abs(move1.z) < Math.abs(move2.z) ? move1.z : move2.z
        );
    }
    
    private static Vec3d getOtherSideMove(
        Entity entity,
        Vec3d attemptedMove,
        Portal collidingPortal,
        Function<Vec3d, Vec3d> handleCollisionFunc,
        Box originalBoundingBox
    ) {
        Box boxOtherSide = getCollisionBoxOtherSide(collidingPortal, originalBoundingBox);
        if (boxOtherSide == null) {
            return attemptedMove;
        }
        
        //switch world and check collision
        World oldWorld = entity.world;
        Vec3d oldPos = entity.getPos();
        Vec3d oldLastTickPos = Helper.lastTickPosOf(entity);
        
        entity.world = getWorld(entity.world.isClient, collidingPortal.dimensionTo);
        entity.setBoundingBox(boxOtherSide);
    
        Vec3d move2 = handleCollisionFunc.apply(attemptedMove);
        
        entity.world = oldWorld;
        Helper.setPosAndLastTickPos(entity, oldPos, oldLastTickPos);
        entity.setBoundingBox(originalBoundingBox);
        
        return move2;
    }
    
    private static Vec3d getThisSideMove(
        Entity entity,
        Vec3d attemptedMove,
        Portal collidingPortal,
        Function<Vec3d, Vec3d> handleCollisionFunc,
        Box originalBoundingBox
    ) {
        Box boxThisSide = getCollisionBoxThisSide(
            collidingPortal, originalBoundingBox
        );
        if (boxThisSide == null) {
            return attemptedMove;
        }
        
        entity.setBoundingBox(boxThisSide);
        Vec3d move1 = handleCollisionFunc.apply(attemptedMove);
    
        entity.setBoundingBox(originalBoundingBox);
    
        return move1;
    }
    
    private static Box getCollisionBoxThisSide(
        Portal portal,
        Box originalBox
    ) {
        //cut the collision box a little bit more for horizontal portals
        //because the box will be stretched by attemptedMove when calculating collision
        Vec3d cullingPos = portal.getNormal().y > 0.5 ?
            portal.getPos().add(portal.getNormal().multiply(0.5)) :
            portal.getPos();
        return clipBox(
            originalBox,
            cullingPos,
            portal.getNormal()
        );
    }
    
    private static Box getCollisionBoxOtherSide(Portal portal, Box originalBox) {
        Vec3d teleportation = portal.destination.subtract(portal.getPos());
        return clipBox(
            originalBox.offset(teleportation),
            portal.destination,
            portal.getNormal().multiply(-1)
        );
    }
    
    public static World getWorld(boolean isClient, DimensionType dimension) {
        if (isClient) {
            return CHelper.getClientWorld(dimension);
        }
        else {
            return Helper.getServer().getWorld(dimension);
        }
    }
    
    //world.getEntities is not reliable
    //it has a small chance to ignore collided entities
    //this would cause player to fall through floor when halfway though portal
    //use entity.getCollidingPortal() and do not use this
    public static Portal getCollidingPortalUnreliable(Entity entity) {
        Box box = entity.getBoundingBox();
        return entity.world.getEntities(
            Portal.class, box, e -> true
        ).stream().filter(
            portal -> shouldCollideWithPortal(
                entity, portal
            )
        ).min(
            Comparator.comparingDouble(p -> p.y)
        ).orElse(null);
    }
    
    public static boolean isCollidingWithAnyPortal(Entity entity) {
        return ((IEEntity) entity).getCollidingPortal() != null;
    }
    
    public static boolean isNearbyPortal(Entity entity) {
        return !entity.world.getEntities(
            Portal.class,
            entity.getBoundingBox().expand(1),
            e -> true
        ).isEmpty();
    }
    
    public static Box getActiveCollisionBox(Entity entity) {
        Portal collidingPortal = ((IEEntity) entity).getCollidingPortal();
        if (collidingPortal != null) {
            Box thisSideBox = getCollisionBoxThisSide(
                collidingPortal,
                entity.getBoundingBox()
            );
            if (thisSideBox != null) {
                return thisSideBox;
            }
            else {
                return new Box(0, 0, 0, 0, 0, 0);
            }
        }
        else {
            return entity.getBoundingBox();
        }
    }
}
