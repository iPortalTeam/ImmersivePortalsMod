package qouteall.imm_ptl.core.teleportation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.ducks.IEEntity;
import qouteall.imm_ptl.core.mixin.common.collision.IEEntity_Collision;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalLike;
import qouteall.imm_ptl.core.portal.global_portals.GlobalPortalStorage;
import qouteall.imm_ptl.core.render.PortalGroup;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.MiscHelper;
import qouteall.q_misc_util.my_util.LimitedLogger;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class CollisionHelper {
    
    private static final LimitedLogger limitedLogger = new LimitedLogger(20);
    
    //cut a box with a plane
    //the facing that normal points to will be remained
    //return null for empty box
    @Nullable
    private static AABB clipBox(AABB box, Vec3 planePos, Vec3 planeNormal) {
        
        boolean xForward = planeNormal.x > 0;
        boolean yForward = planeNormal.y > 0;
        boolean zForward = planeNormal.z > 0;
        
        Vec3 pushedPos = new Vec3(
            xForward ? box.minX : box.maxX,
            yForward ? box.minY : box.maxY,
            zForward ? box.minZ : box.maxZ
        );
        Vec3 staticPos = new Vec3(
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
        Vec3 afterBeingPushed = pushedPos.add(planeNormal.scale(tOfPushedPos));
        return new AABB(afterBeingPushed, staticPos);
    }
    
    
    public static boolean isBoxBehindPlane(Vec3 planePos, Vec3 planeNormal, AABB box) {
        boolean xForward = planeNormal.x > 0;
        boolean yForward = planeNormal.y > 0;
        boolean zForward = planeNormal.z > 0;
        
        Vec3 testingPos = new Vec3(
            xForward ? box.maxX : box.minX,
            yForward ? box.maxY : box.minY,
            zForward ? box.maxZ : box.minZ
        );
        
        return testingPos.subtract(planePos).dot(planeNormal) < 0;
    }
    
    public static boolean canCollideWithPortal(Entity entity, Portal portal, float tickDelta) {
        if (portal.canTeleportEntity(entity)) {
            Vec3 cameraPosVec = entity.getEyePosition(tickDelta);
            if (portal.isInFrontOfPortal(cameraPosVec)) {
                PortalLike collisionHandlingUnit = getCollisionHandlingUnit(portal);
                boolean isInGroup = collisionHandlingUnit != portal;
                if (isInGroup) {
                    return true;
                }
                if (portal.isPointInPortalProjection(cameraPosVec)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    public static Vec3 handleCollisionHalfwayInPortal(
        Entity entity,
        Vec3 attemptedMove,
        Portal collidingPortal,
        Function<Vec3, Vec3> handleCollisionFunc
    ) {
        entity.level.getProfiler().push("cross_portal_collision");
        
        AABB originalBoundingBox = entity.getBoundingBox();
        
        Vec3 thisSideMove = getThisSideMove(
            entity, attemptedMove, collidingPortal,
            originalBoundingBox
        );
        
        if (thisSideMove.y > 0 && attemptedMove.y < 0) {
            //stepping onto slab on this side
            //re-calc collision with intact collision box
            //the stepping is shorter using the clipped collision box
            thisSideMove = handleCollisionFunc.apply(attemptedMove);
        }
        
        Vec3 otherSideMove = getOtherSideMove(
            entity, thisSideMove, getCollisionHandlingUnit(collidingPortal),
            handleCollisionFunc, originalBoundingBox
        );
        
        entity.level.getProfiler().pop();
        
        return new Vec3(
            correctXZCoordinate(attemptedMove.x, otherSideMove.x),
            correctYCoordinate(attemptedMove.y, otherSideMove.y),
            correctXZCoordinate(attemptedMove.z, otherSideMove.z)
        );
    }
    
    private static double absMin(double a, double b) {
        return Math.abs(a) < Math.abs(b) ? a : b;
    }
    
    private static Vec3 getOtherSideMove(
        Entity entity,
        Vec3 attemptedMove,
        PortalLike collidingPortal,
        Function<Vec3, Vec3> handleCollisionFunc,
        AABB originalBoundingBox
    ) {
        if (!collidingPortal.getHasCrossPortalCollision()) {
            return attemptedMove;
        }
        
        Vec3 transformedAttemptedMove = collidingPortal.transformLocalVec(attemptedMove);
        
        AABB boxOtherSide = getCollisionBoxOtherSide(
            collidingPortal,
            originalBoundingBox,
            transformedAttemptedMove
        );
        if (boxOtherSide == null) {
            return attemptedMove;
        }
        
        Level destinationWorld = collidingPortal.getDestWorld();
        
        if (!destinationWorld.hasChunkAt(new BlockPos(boxOtherSide.getCenter()))) {
            if (entity instanceof Player && entity.level.isClientSide()) {
                informClientStagnant();
            }
            return Vec3.ZERO;
        }
        
        //switch world and check collision
        Level oldWorld = entity.level;
        Vec3 oldPos = entity.position();
        Vec3 oldLastTickPos = McHelper.lastTickPosOf(entity);
        float oldStepHeight = entity.maxUpStep;
        
        entity.level = destinationWorld;
        entity.setBoundingBox(boxOtherSide);
        if (collidingPortal.getScale() > 1) {
            entity.maxUpStep = (float) (oldStepHeight * collidingPortal.getScale() * 2);
        }
        
        Vec3 collided = handleCollisionFunc.apply(transformedAttemptedMove);
        
        collided = new Vec3(
            correctXZCoordinate(transformedAttemptedMove.x, collided.x),
            correctYCoordinate(transformedAttemptedMove.y, collided.y),
            correctXZCoordinate(transformedAttemptedMove.z, collided.z)
        );
        
        Vec3 result = collidingPortal.inverseTransformLocalVec(collided);
        
        entity.level = oldWorld;
        McHelper.setPosAndLastTickPos(entity, oldPos, oldLastTickPos);
        entity.setBoundingBox(originalBoundingBox);
        entity.maxUpStep = oldStepHeight;
        
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
    
    private static Vec3 getThisSideMove(
        Entity entity,
        Vec3 attemptedMove,
        Portal collidingPortal,
        AABB originalBoundingBox
    ) {
        AABB boundingBox = entity.getBoundingBox();
        AABB expandedBox = boundingBox.expandTowards(attemptedMove);
        List<VoxelShape> entityCollisions = entity.level.getEntityCollisions(entity, expandedBox);
        Iterable<VoxelShape> blockCollisions = entity.level.getBlockCollisions(entity, expandedBox);
        
        List<VoxelShape> validCollisions = new ArrayList<>();
        for (VoxelShape entityCollision : entityCollisions) {
            boolean boxBehindPlane = isBoxBehindPlane(
                collidingPortal.getOriginPos(), collidingPortal.getNormal(), entityCollision.bounds()
            );
            if (!boxBehindPlane) {
                validCollisions.add(entityCollision);
            }
        }
        
        for (VoxelShape blockCollision : blockCollisions) {
            boolean boxBehindPlane = isBoxBehindPlane(
                collidingPortal.getOriginPos(), collidingPortal.getNormal(), blockCollision.bounds()
            );
            if (!boxBehindPlane) {
                validCollisions.add(blockCollision);
            }
        }
        
        Vec3 result = IEEntity_Collision.ip_CollideWithShapes(attemptedMove, boundingBox, validCollisions);
        return result;
    }
    
    private static AABB getCollisionBoxThisSide(
        Portal portal,
        AABB originalBox,
        Vec3 attemptedMove
    ) {
        //cut the collision box a little bit more for horizontal portals
        //because the box will be stretched by attemptedMove when calculating collision
        Vec3 clippingPos = portal.getOriginPos().subtract(attemptedMove);
        return clipBox(
            originalBox,
            clippingPos,
            portal.getNormal()
        );
    }
    
    private static AABB getCollisionBoxOtherSide(
        PortalLike portalLike,
        AABB originalBox,
        Vec3 transformedAttemptedMove
    ) {
        if (portalLike instanceof Portal) {
            Portal portal = (Portal) portalLike;
            
            AABB otherSideBox = transformBox(portal, originalBox);
            
            Vec3 clippingPos = portal.getDestPos().subtract(transformedAttemptedMove);
            
            AABB box = clipBox(
                otherSideBox,
                clippingPos,
                portal.getContentDirection()
            );
            
            if (box != null) {
                // special handling in high speed (not perfect)
                Vec3 contentDirection = portal.getContentDirection();
                boolean movingIntoPortal =
                    contentDirection.dot(transformedAttemptedMove) > 0;
                
                if (movingIntoPortal && transformedAttemptedMove.lengthSqr() > 1) {
                    // the box is not clipped
                    if (box.getCenter().subtract(otherSideBox.getCenter()).lengthSqr() < 0.2) {
                        // avoid colliding with blocks behind the portal destination
                        box = box.move(
                            contentDirection.scale(transformedAttemptedMove.dot(contentDirection))
                        );
                    }
                }
            }
            
            return box;
        } else {
            PortalGroup portalGroup = (PortalGroup) portalLike;
            return transformBox(portalGroup.getFirstPortal(), originalBox);
        }
    }
    
    private static AABB transformBox(Portal portal, AABB originalBox) {
        if (portal.rotation == null && portal.scaling == 1) {
            return originalBox.move(portal.getDestPos().subtract(portal.getOriginPos()));
        } else {
            return Helper.transformBox(originalBox, portal::transformPoint);
        }
    }
    
    public static Level getWorld(boolean isClient, ResourceKey<Level> dimension) {
        if (isClient) {
            return CHelper.getClientWorld(dimension);
        } else {
            return MiscHelper.getServer().getLevel(dimension);
        }
    }
    
    public static boolean isCollidingWithAnyPortal(Entity entity) {
        return ((IEEntity) entity).getCollidingPortal() != null;
    }
    
    public static AABB getActiveCollisionBox(Entity entity) {
        Portal collidingPortal = ((IEEntity) entity).getCollidingPortal();
        if (collidingPortal != null) {
            AABB thisSideBox = getCollisionBoxThisSide(
                collidingPortal,
                entity.getBoundingBox(),
                Vec3.ZERO //is it ok?
            );
            if (thisSideBox != null) {
                return thisSideBox;
            } else {
                return new AABB(0, 0, 0, 0, 0, 0);
            }
        } else {
            return entity.getBoundingBox();
        }
    }
    
    private static void updateGlobalPortalCollidingPortalForWorld(Level world) {
        world.getProfiler().push("global_portal_colliding_portal");
        
        List<Portal> globalPortals = GlobalPortalStorage.getGlobalPortals(world);
        Iterable<Entity> worldEntityList = McHelper.getWorldEntityList(world);
        
        if (!globalPortals.isEmpty()) {
            for (Entity entity : worldEntityList) {
                AABB entityBoundingBoxStretched = getStretchedBoundingBox(entity);
                for (Portal globalPortal : globalPortals) {
                    AABB globalPortalBoundingBox = globalPortal.getBoundingBox();
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
        IPGlobal.postServerTickSignal.connect(() -> {
            for (ServerLevel world : MiscHelper.getServer().getAllLevels()) {
                updateGlobalPortalCollidingPortalForWorld(world);
            }
        });
    }
    
    @Environment(EnvType.CLIENT)
    public static void initClient() {
        IPGlobal.postClientTickSignal.connect(CollisionHelper::tickClient);
    }
    
    @Environment(EnvType.CLIENT)
    public static void tickClient() {
        updateGlobalPortalCollidingStatus();
        
        updateClientStagnateStatus();
    }
    
    @Environment(EnvType.CLIENT)
    private static void updateGlobalPortalCollidingStatus() {
        if (ClientWorldLoader.getIsInitialized()) {
            for (ClientLevel world : ClientWorldLoader.getClientWorlds()) {
                updateGlobalPortalCollidingPortalForWorld(world);
            }
        }
    }
    
    public static void notifyCollidingPortals(Portal portal) {
        if (!portal.teleportable) {
            return;
        }
        
        AABB portalBoundingBox = portal.getBoundingBox();
        
        McHelper.foreachEntitiesByBoxApproximateRegions(
            Entity.class, portal.level,
            portalBoundingBox, 3,
            entity -> {
                if (entity instanceof Portal) {
                    return;
                }
                AABB entityBoxStretched = getStretchedBoundingBox(entity);
                if (!entityBoxStretched.intersects(portalBoundingBox)) {
                    return;
                }
                boolean canCollideWithPortal = canCollideWithPortal(entity, portal, 1);
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
        
        entity.level.getProfiler().push("update_colliding_portal_now");
        
        AABB boundingBox = getStretchedBoundingBox(entity);
        McHelper.foreachEntitiesByBoxApproximateRegions(
            Portal.class,
            entity.level,
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
        
        entity.level.getProfiler().pop();
    }
    
    public static AABB getStretchedBoundingBox(Entity entity) {
        // normal colliding portal update lags 1 tick before collision calculation
        // the velocity updates later after updating colliding portal
        // expand the velocity to avoid not collide with portal in time
        Vec3 expand = McHelper.getWorldVelocity(entity).scale(1.2);
        return entity.getBoundingBox().expandTowards(expand);
    }
    
    private static boolean thisTickStagnate = false;
    private static boolean lastTickStagnate = false;
    
    @Environment(EnvType.CLIENT)
    private static void informClientStagnant() {
        thisTickStagnate = true;
        limitedLogger.log("client movement stagnated");
    }
    
    @Environment(EnvType.CLIENT)
    private static void updateClientStagnateStatus() {
        if (thisTickStagnate && lastTickStagnate) {
            Minecraft.getInstance().gui.setOverlayMessage(
                Component.translatable("imm_ptl.stagnate_movement"),
                false
            );
        } else if (!thisTickStagnate && lastTickStagnate) {
            Minecraft.getInstance().gui.setOverlayMessage(
                Component.literal(""),
                false
            );
        }
        
        lastTickStagnate = thisTickStagnate;
        thisTickStagnate = false;
    }
    
    public static PortalLike getCollisionHandlingUnit(Portal portal) {
        if (portal.getIsGlobal()) {
            return portal;
        }
        if (portal.level.isClientSide()) {
            return getCollisionHandlingUnitClient(portal);
        } else {
            return portal;
        }
    }
    
    @Environment(EnvType.CLIENT)
    private static PortalLike getCollisionHandlingUnitClient(Portal portal) {
        return PortalGroup.getPortalUnit(portal);
    }
}
