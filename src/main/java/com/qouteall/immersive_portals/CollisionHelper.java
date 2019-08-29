package com.qouteall.immersive_portals;

import com.qouteall.immersive_portals.my_util.Helper;
import com.qouteall.immersive_portals.portal.Portal;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

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
        if (!entity.getBoundingBox().intersects(portal.getBoundingBox())) {
            return false;
        }
        boolean result = portal.isInFrontOfPortal(entity.getPos().add(
            0, entity.getStandingEyeHeight(), 0
        ));
        return result;
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
        
        //return move1;
        
        Vec3d move2 = getOtherSideMove(
            entity, attemptedMove, collidingPortal,
            handleCollisionFunc, originalBoundingBox
        );
        
        //restore bounding box
        entity.setBoundingBox(originalBoundingBox);
        
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
            collidingPortal, originalBoundingBox, attemptedMove
        );
        if (boxThisSide == null) {
            return attemptedMove;
        }
        
        entity.setBoundingBox(boxThisSide);
        return handleCollisionFunc.apply(attemptedMove);
    }
    
    private static Box getCollisionBoxThisSide(
        Portal portal,
        Box originalBox,
        Vec3d attemptedMove
    ) {
        return clipBox(
            originalBox,
            //cut the collision box a little bit more
            //because the box will be stretched by attemptedMove when calculating collision
            portal.getPos().add(portal.getNormal().multiply(0.4)),
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
            return CGlobal.clientWorldLoader.getOrCreateFakedWorld(dimension);
        }
        else {
            return Helper.getServer().getWorld(dimension);
        }
    }
    
    public static Portal getCollidingPortal(Entity entity, double expandFactor) {
        return entity.world.getEntities(
            Portal.class, entity.getBoundingBox().expand(expandFactor), e -> true
        ).stream().filter(
            portal -> shouldCollideWithPortal(
                entity, portal
            )
        ).findFirst().orElse(null);
    }
}
