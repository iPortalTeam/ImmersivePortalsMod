package com.qouteall.immersive_portals.teleportation;

import com.qouteall.immersive_portals.CHelper;
import com.qouteall.immersive_portals.ClientWorldLoader;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.ducks.IEEntity;
import com.qouteall.immersive_portals.my_util.LimitedLogger;
import com.qouteall.immersive_portals.portal.Portal;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Function;

public class CollisionHelper {
    
    private static final LimitedLogger limitedLogger = new LimitedLogger(20);
    
    //cut a box with a plane
    //the facing that normal points to will be remained
    //return null for empty box
    @Nullable
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
    
    public static boolean canCollideWithPortal(Entity entity, Portal portal, float tickDelta) {
        if (portal.canTeleportEntity(entity)) {
            Vec3d cameraPosVec = entity.getCameraPosVec(tickDelta);
            if (portal.isInFrontOfPortal(cameraPosVec) &&
                portal.isPointInPortalProjection(cameraPosVec)) {
                return true;
            }
        }
        return false;
    }
    
    public static Vec3d handleCollisionHalfwayInPortal(
        Entity entity,
        Vec3d attemptedMove,
        Portal collidingPortal,
        Function<Vec3d, Vec3d> handleCollisionFunc
    ) {
        entity.world.getProfiler().push("cross_portal_collision");
        
        Box originalBoundingBox = entity.getBoundingBox();
        
        Vec3d thisSideMove = getThisSideMove(
            entity, attemptedMove, collidingPortal,
            handleCollisionFunc, originalBoundingBox
        );
        
        if (thisSideMove.y > 0 && attemptedMove.y < 0) {
            //stepping onto slab on this side
            //re-calc collision with intact collision box
            //the stepping is shorter using the clipped collision box
            thisSideMove = handleCollisionFunc.apply(attemptedMove);
        }
        
        Vec3d otherSideMove = getOtherSideMove(
            entity, thisSideMove, collidingPortal,
            handleCollisionFunc, originalBoundingBox
        );
        
        entity.world.getProfiler().pop();
        
        return new Vec3d(
            correctXZCoordinate(attemptedMove.x, otherSideMove.x),
            correctYCoordinate(attemptedMove.y, otherSideMove.y),
            correctXZCoordinate(attemptedMove.z, otherSideMove.z)
        );
    }
    
    private static double absMin(double a, double b) {
        return Math.abs(a) < Math.abs(b) ? a : b;
    }
    
    private static Vec3d getOtherSideMove(
        Entity entity,
        Vec3d attemptedMove,
        Portal collidingPortal,
        Function<Vec3d, Vec3d> handleCollisionFunc,
        Box originalBoundingBox
    ) {
        if (!collidingPortal.hasCrossPortalCollision) {
            return attemptedMove;
        }
        
        Vec3d transformedAttemptedMove = collidingPortal.transformCollisionMovement(attemptedMove);
        
        Box boxOtherSide = getCollisionBoxOtherSide(
            collidingPortal,
            originalBoundingBox,
            transformedAttemptedMove
        );
        if (boxOtherSide == null) {
            return attemptedMove;
        }
        
        World destinationWorld = collidingPortal.getDestinationWorld();
        
        if (!destinationWorld.isChunkLoaded(new BlockPos(boxOtherSide.getCenter()))) {
            if (entity instanceof PlayerEntity && entity.world.isClient()) {
                informClientStagnant();
            }
            return Vec3d.ZERO;
        }
        
        //switch world and check collision
        World oldWorld = entity.world;
        Vec3d oldPos = entity.getPos();
        Vec3d oldLastTickPos = McHelper.lastTickPosOf(entity);
        float oldStepHeight = entity.stepHeight;
        
        entity.world = destinationWorld;
        entity.setBoundingBox(boxOtherSide);
        if (collidingPortal.scaling > 1) {
            entity.stepHeight = (float) (oldStepHeight * collidingPortal.scaling * 2);
        }
        
        Vec3d collided = handleCollisionFunc.apply(transformedAttemptedMove);
        
        collided = new Vec3d(
            correctXZCoordinate(transformedAttemptedMove.x, collided.x),
            correctYCoordinate(transformedAttemptedMove.y, collided.y),
            correctXZCoordinate(transformedAttemptedMove.z, collided.z)
        );
        
        Vec3d result = collidingPortal.inverseTransformCollisionMovement(collided);
        
        entity.world = oldWorld;
        McHelper.setPosAndLastTickPos(entity, oldPos, oldLastTickPos);
        entity.setBoundingBox(originalBoundingBox);
        entity.stepHeight = oldStepHeight;
        
        return result;
    }
    
    // floating point deviation may cause collision issues
    private static double correctXZCoordinate(double attemptedMove, double result) {
        //rotation may cause a free move to reduce a little bit and the game think that it's collided
        if (Math.abs(attemptedMove - result) < 0.001) {
            return attemptedMove;
        }
        
        //0 may become 0.0000001 after rotation. avoid falling through floor
        if (Math.abs(result) < 0.0001) {
            return 0;
        }
        
        //pushing away
        if (Math.abs(result) > Math.abs(attemptedMove) + 0.01) {
            return result;
        }
        
        //1 may become 0.999999 after rotation. avoid going into wall
        return result * 0.999;
    }
    
    private static double correctYCoordinate(double attemptedMove, double result) {
        if (Math.abs(attemptedMove - result) < 0.001) {
            return attemptedMove;
        }
        
        //0 may become 0.0000001 after rotation. avoid falling through floor
        if (Math.abs(result) < 0.0001) {
            return 0;
        }
        
        //pushing away
        if (Math.abs(result) > Math.abs(attemptedMove) + 0.01) {
            return result;
        }
        
        if (result < 0) {
            return result * 0.999;//stepping onto floor won't get reduced
        }
        
        return result;
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
        Vec3d move1 = handleCollisionFunc.apply(attemptedMove);
        
        entity.setBoundingBox(originalBoundingBox);
        
        return move1;
    }
    
    private static Box getCollisionBoxThisSide(
        Portal portal,
        Box originalBox,
        Vec3d attemptedMove
    ) {
        //cut the collision box a little bit more for horizontal portals
        //because the box will be stretched by attemptedMove when calculating collision
        Vec3d clippingPos = portal.getOriginPos().subtract(attemptedMove);
        return clipBox(
            originalBox,
            clippingPos,
            portal.getNormal()
        );
    }
    
    private static Box getCollisionBoxOtherSide(
        Portal portal,
        Box originalBox,
        Vec3d transformedAttemptedMove
    ) {
        Box otherSideBox = transformBox(portal, originalBox);
        
        Vec3d clippingPos = portal.getDestPos().subtract(transformedAttemptedMove);
        
        Box box = clipBox(
            otherSideBox,
            clippingPos,
            portal.getContentDirection()
        );
        
        if (box != null) {
            Vec3d contentDirection = portal.getContentDirection();
            boolean movingIntoPortal =
                contentDirection.dotProduct(transformedAttemptedMove) > 0;
            if (movingIntoPortal && transformedAttemptedMove.lengthSquared() > 1) {
                // the box is not clipped
                if (box.getCenter().subtract(otherSideBox.getCenter()).lengthSquared() < 0.2) {
                    // avoid colliding with blocks behind the portal destination
                    box = box.offset(
                        contentDirection.multiply(transformedAttemptedMove.dotProduct(contentDirection))
                    );
                }
            }
        }
        
        return box;
    }
    
    private static Box transformBox(Portal portal, Box originalBox) {
        if (portal.rotation == null && portal.scaling == 1) {
            return originalBox.offset(portal.getDestPos().subtract(portal.getOriginPos()));
        }
        else {
//            if (portal.teleportChangesScale) {
            return Helper.transformBox(originalBox, portal::transformPoint);
//            }
//            else {
//                Vec3d boxSize = Helper.getBoxSize(originalBox);
//                Vec3d centerToAnchor =
//                    portal.getNormal().multiply(boxSize.dotProduct(portal.getNormal()) * -0.5);
//                Vec3d boxCenter = originalBox.getCenter();
//                Vec3d boxAnchor = boxCenter.add(centerToAnchor);
//                Box boxMovedToOrigin = originalBox.offset(boxAnchor.multiply(-1));
//                Box rotatedBox = Helper.transformBox(boxMovedToOrigin, portal::transformLocalVecNonScale);
//                Vec3d transformedBoxCenter = portal.transformPoint(boxAnchor);
//                return rotatedBox.offset(transformedBoxCenter);
//            }
        }
    }
    
    public static World getWorld(boolean isClient, RegistryKey<World> dimension) {
        if (isClient) {
            return CHelper.getClientWorld(dimension);
        }
        else {
            return McHelper.getServer().getWorld(dimension);
        }
    }
    
    public static boolean isCollidingWithAnyPortal(Entity entity) {
        return ((IEEntity) entity).getCollidingPortal() != null;
    }
    
    public static Box getActiveCollisionBox(Entity entity) {
        Portal collidingPortal = ((IEEntity) entity).getCollidingPortal();
        if (collidingPortal != null) {
            Box thisSideBox = getCollisionBoxThisSide(
                collidingPortal,
                entity.getBoundingBox(),
                Vec3d.ZERO //is it ok?
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
    
    private static void updateGlobalPortalCollidingPortalForWorld(World world) {
        world.getProfiler().push("global_portal_colliding_portal");
        
        List<Portal> globalPortals = McHelper.getGlobalPortals(world);
        Iterable<Entity> worldEntityList = McHelper.getWorldEntityList(world);
        
        if (!globalPortals.isEmpty()) {
            for (Entity entity : worldEntityList) {
                Box entityBoundingBoxStretched = getStretchedBoundingBox(entity);
                for (Portal globalPortal : globalPortals) {
                    Box globalPortalBoundingBox = globalPortal.getBoundingBox();
                    if (entityBoundingBoxStretched.intersects(globalPortalBoundingBox)) {
                        if (canCollideWithPortal(entity, globalPortal, 0)) {
                            ((IEEntity) entity).notifyCollidingWithPortal(globalPortal);
                        }
                    }
                }
            }
        }
        
        world.getProfiler().pop();
    }
    
    public static void init() {
        ModMain.postServerTickSignal.connect(() -> {
            for (ServerWorld world : McHelper.getServer().getWorlds()) {
                updateGlobalPortalCollidingPortalForWorld(world);
            }
        });
    }
    
    @Environment(EnvType.CLIENT)
    public static void initClient() {
        ModMain.postClientTickSignal.connect(CollisionHelper::updateClientGlobalPortalCollidingPortal);
    }
    
    @Environment(EnvType.CLIENT)
    public static void updateClientGlobalPortalCollidingPortal() {
        if (ClientWorldLoader.getIsInitialized()) {
            for (ClientWorld world : ClientWorldLoader.getClientWorlds()) {
                updateGlobalPortalCollidingPortalForWorld(world);
            }
        }
    }
    
    public static void notifyCollidingPortals(Portal portal) {
        if (!portal.teleportable) {
            return;
        }
        
        Box portalBoundingBox = portal.getBoundingBox();
        
        McHelper.foreachEntitiesByBoxApproximateRegions(
            Entity.class, portal.world,
            portalBoundingBox, 3,
            entity -> {
                if (entity instanceof Portal) {
                    return;
                }
                Box entityBoxStretched = getStretchedBoundingBox(entity);
                if (!entityBoxStretched.intersects(portalBoundingBox)) {
                    return;
                }
                boolean canCollideWithPortal = canCollideWithPortal(entity, portal, 0);
                if (!canCollideWithPortal) {
                    return;
                }
                
                ((IEEntity) entity).notifyCollidingWithPortal(portal);
            }
        );
    }
    
    // the normal way of updating colliding portal delays one tick and does not work in high speed
    // to save performance, only do this on high speed
    public static void updateCollidingPortalNow(Entity entity) {
        if (entity instanceof Portal) {
            return;
        }
        
        entity.world.getProfiler().push("update_colliding_portal_now");
        
        Box boundingBox = getStretchedBoundingBox(entity);
        McHelper.foreachEntitiesByBoxApproximateRegions(
            Portal.class,
            entity.world,
            boundingBox,
            10,
            portal -> {
                if (boundingBox.intersects(portal.getBoundingBox())) {
                    if (canCollideWithPortal(entity, portal, 0)) {
                        ((IEEntity) entity).notifyCollidingWithPortal(portal);
                    }
                }
            }
        );
        
        entity.world.getProfiler().pop();
    }
    
    public static Box getStretchedBoundingBox(Entity entity) {
        // normal colliding portal update lags 1 tick before collision calculation
        // the velocity updates later after updating colliding portal
        // expand the velocity to avoid not collide with portal in time
        Vec3d expand = entity.getVelocity().multiply(1.2);
        return entity.getBoundingBox().stretch(expand);
    }
    
    @Environment(EnvType.CLIENT)
    private static void informClientStagnant() {
        MinecraftClient.getInstance().inGameHud.setOverlayMessage(
            new TranslatableText("imm_ptl.stagnate_movement"),
            true
        );
    }
}
