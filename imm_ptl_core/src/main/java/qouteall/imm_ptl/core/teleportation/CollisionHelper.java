package qouteall.imm_ptl.core.teleportation;

import com.google.common.collect.ImmutableList;
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
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.ducks.IEEntity;
import qouteall.imm_ptl.core.miscellaneous.IPVanillaCopy;
import qouteall.imm_ptl.core.mixin.common.collision.IEEntity_Collision;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalLike;
import qouteall.imm_ptl.core.portal.global_portals.GlobalPortalStorage;
import qouteall.imm_ptl.core.render.PortalGroup;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.MiscHelper;
import qouteall.q_misc_util.my_util.LimitedLogger;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

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
    
    
    public static boolean isBoxFullyBehindPlane(Vec3 planePos, Vec3 planeNormal, AABB box) {
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
    
    public static boolean canCollideWithPortal(Entity entity, Portal portal, float partialTick) {
        if (portal.canTeleportEntity(entity)) {
            Vec3 cameraPosVec = entity.getEyePosition(partialTick);
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
        Portal collidingPortal
    ) {
        entity.level.getProfiler().push("cross_portal_collision");
        
        AABB originalBoundingBox = entity.getBoundingBox();
        
        Vec3 thisSideMove = getThisSideMove(
            entity, attemptedMove, collidingPortal,
            originalBoundingBox
        );
        
        Vec3 otherSideMove = getOtherSideMove(
            entity, thisSideMove, collidingPortal,
            originalBoundingBox, 1
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
        Portal collidingPortal,
        AABB originalBoundingBox,
        int portalLayer
    ) {
        // limit max recursion layer
        if (portalLayer >= 100) {
            return attemptedMove;
        }
        
        if (!collidingPortal.getHasCrossPortalCollision()) {
            return attemptedMove;
        }
        
        Vec3 transformedAttemptedMove = collidingPortal.transformLocalVec(attemptedMove);
        
        AABB boxOtherSide = transformBox(collidingPortal, originalBoundingBox);
        if (boxOtherSide == null) {
            return attemptedMove;
        }
        
        Level destinationWorld = collidingPortal.getDestWorld();
        
        if (!destinationWorld.hasChunkAt(new BlockPos(boxOtherSide.getCenter()))) {
            if (entity instanceof Player && entity.level.isClientSide()) {
                informClientStagnant();
            }
            Vec3 innerDirection = collidingPortal.getNormal().scale(-1);
            
            if (attemptedMove.dot(innerDirection) < 0) {
                return attemptedMove;
            }
            else {
                return attemptedMove.subtract(innerDirection.scale(innerDirection.dot(attemptedMove)));
            }
        }
        
        List<Portal> indirectCollidingPortals = McHelper.findEntitiesByBox(
            Portal.class,
            collidingPortal.getDestinationWorld(),
            boxOtherSide.expandTowards(transformedAttemptedMove),
            8,
            p -> p.getHasCrossPortalCollision()
                && canCollideWithPortal(entity, p, 0)
                && !Portal.isReversePortal(collidingPortal, p)
                && !Portal.isParallelPortal(collidingPortal, p)
                && Portal.isFlippedPortal(collidingPortal, p)
        );
        
        //switch world and check collision
        Level oldWorld = entity.level;
        Vec3 oldPos = entity.position();
        Vec3 oldLastTickPos = McHelper.lastTickPosOf(entity);
        float oldStepHeight = entity.maxUpStep;
        
        entity.level = destinationWorld;
        entity.setBoundingBox(boxOtherSide);
        if (collidingPortal.getScale() > 1) {
            entity.maxUpStep = (float) (oldStepHeight * collidingPortal.getScale() * 1.01);
        }
        
        try {
            if (!indirectCollidingPortals.isEmpty()) {
                return getOtherSideMove(
                    entity, transformedAttemptedMove, indirectCollidingPortals.get(0),
                    entity.getBoundingBox(), portalLayer + 1
                );
            }
            
            Vec3 collided = handleCollisionWithClipping(
                entity, transformedAttemptedMove,
                collidingPortal.getDestPos(), collidingPortal.getContentDirection()
            );
            
            collided = new Vec3(
                correctXZCoordinate(transformedAttemptedMove.x, collided.x),
                correctYCoordinate(transformedAttemptedMove.y, collided.y),
                correctXZCoordinate(transformedAttemptedMove.z, collided.z)
            );
            
            Vec3 result = collidingPortal.inverseTransformLocalVec(collided);
            
            return result;
        } finally {
            entity.level = oldWorld;
            McHelper.setPosAndLastTickPos(entity, oldPos, oldLastTickPos);
            entity.setBoundingBox(originalBoundingBox);
            entity.maxUpStep = oldStepHeight;
        }
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
        Vec3 clippingPlanePos = collidingPortal.getOriginPos();
        Vec3 clippingPlaneNormal = collidingPortal.getNormal();
        
        return handleCollisionWithClipping(entity, attemptedMove, clippingPlanePos, clippingPlaneNormal);
    }
    
    /**
     * Vanilla copy {@link Entity#collide(Vec3)}
     * But filters collisions behind the clipping plane
     */
    @IPVanillaCopy
    private static Vec3 handleCollisionWithClipping(
        Entity entity, Vec3 attemptedMove, Vec3 clippingPlanePos, Vec3 clippingPlaneNormal
    ) {
        Function<VoxelShape, VoxelShape> filter = shape -> {
            AABB shapeBoundingBox = shape.bounds();
            boolean boxBehindPlane = isBoxFullyBehindPlane(
                clippingPlanePos, clippingPlaneNormal, shapeBoundingBox
            );
            if (boxBehindPlane) {
                return null;
            }
            
            boolean isFullyInFrontOfPlane = isBoxFullyBehindPlane(
                clippingPlanePos, clippingPlaneNormal.scale(-1), shapeBoundingBox
            );
            
            if (isFullyInFrontOfPlane) {
                return shape;
            }
            
            // the shape is intersecting the clipping plane
            // clip the shape
            AABB clippedBoundingBox = clipBox(
                shapeBoundingBox, clippingPlanePos, clippingPlaneNormal
            );
            
            if (clippedBoundingBox == null) {
                return null;
            }
            
            VoxelShape result = Shapes.joinUnoptimized(
                shape,
                Shapes.create(clippedBoundingBox),
                BooleanOp.AND
            );
            
            return result;
        };
        
        AABB boundingBox = entity.getBoundingBox();
        List<VoxelShape> entityCollisions = entity.level.getEntityCollisions(entity, boundingBox.expandTowards(attemptedMove));
        Vec3 collidedMovement = attemptedMove.lengthSqr() == 0.0D ? attemptedMove : collideBoundingBox(
            entity, attemptedMove, boundingBox, entity.level, entityCollisions, filter
        );
        boolean moveX = attemptedMove.x != collidedMovement.x;
        boolean moveY = attemptedMove.y != collidedMovement.y;
        boolean moveZ = attemptedMove.z != collidedMovement.z;
        boolean touchGround = entity.isOnGround() || moveY && attemptedMove.y < 0.0D;
        if (entity.maxUpStep > 0.0F && touchGround && (moveX || moveZ)) {
            Vec3 stepping = collideBoundingBox(
                entity,
                new Vec3(attemptedMove.x, (double) entity.maxUpStep, attemptedMove.z),
                boundingBox, entity.level, entityCollisions, filter
            );
            Vec3 verticalStep = collideBoundingBox(
                entity, new Vec3(0.0D, (double) entity.maxUpStep, 0.0D),
                boundingBox.expandTowards(attemptedMove.x, 0.0D, attemptedMove.z),
                entity.level, entityCollisions, filter
            );
            if (verticalStep.y < (double) entity.maxUpStep) {
                Vec3 horizontalMoveAfterVerticalStepping = collideBoundingBox(
                    entity, new Vec3(attemptedMove.x, 0.0D, attemptedMove.z),
                    boundingBox.move(verticalStep), entity.level, entityCollisions, filter
                ).add(verticalStep);
                if (horizontalMoveAfterVerticalStepping.horizontalDistanceSqr() > stepping.horizontalDistanceSqr()) {
                    stepping = horizontalMoveAfterVerticalStepping;
                }
            }
            
            if (stepping.horizontalDistanceSqr() > collidedMovement.horizontalDistanceSqr()) {
                Vec3 moveAfterStepping = collideBoundingBox(
                    entity,
                    new Vec3(0.0D, -stepping.y + attemptedMove.y, 0.0D),
                    boundingBox.move(stepping),
                    entity.level,
                    entityCollisions,
                    filter
                );
                return stepping.add(moveAfterStepping);
            }
        }
        
        return collidedMovement;
    }
    
    /**
     * Vanilla copy {@link Entity#collideBoundingBox(Entity, Vec3, AABB, Level, List)}
     * But filters collisions behind the clipping plane
     */
    @IPVanillaCopy
    public static Vec3 collideBoundingBox(
        Entity entity, Vec3 vec,
        AABB collisionBox, Level level,
        List<VoxelShape> potentialHits,
        Function<VoxelShape, VoxelShape> shapeProcessor
    ) {
        ImmutableList.Builder<VoxelShape> builder =
            ImmutableList.builderWithExpectedSize(potentialHits.size() + 1);
        
        for (VoxelShape potentialHit : potentialHits) {
            VoxelShape processed = shapeProcessor.apply(potentialHit);
            if (processed != null) {
                builder.add(processed);
            }
        }
        
        WorldBorder worldBorder = level.getWorldBorder();
        boolean isCloseToWorldBorder = worldBorder.isInsideCloseToBorder(entity, collisionBox.expandTowards(vec));
        if (isCloseToWorldBorder) {
            builder.add(worldBorder.getCollisionShape());
        }
        
        Iterable<VoxelShape> blockCollisions = level.getBlockCollisions(entity, collisionBox.expandTowards(vec));
        
        for (VoxelShape blockCollision : blockCollisions) {
            VoxelShape processed = shapeProcessor.apply(blockCollision);
            if (processed != null) {
                builder.add(processed);
            }
        }
        
        return IEEntity_Collision.ip_CollideWithShapes(vec, collisionBox, builder.build());
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
    
    @Deprecated
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
        }
        else {
            PortalGroup portalGroup = (PortalGroup) portalLike;
            return transformBox(portalGroup.getFirstPortal(), originalBox);
        }
    }
    
    private static AABB transformBox(PortalLike portal, AABB originalBox) {
        if (portal.getRotation() == null && portal.getScale() == 1) {
            return originalBox.move(portal.getDestPos().subtract(portal.getOriginPos()));
        }
        else {
            return Helper.transformBox(originalBox, portal::transformPoint);
        }
    }
    
    public static Level getWorld(boolean isClient, ResourceKey<Level> dimension) {
        if (isClient) {
            return CHelper.getClientWorld(dimension);
        }
        else {
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
            }
            else {
                return new AABB(0, 0, 0, 0, 0, 0);
            }
        }
        else {
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
    
    public static void notifyCollidingPortals(Portal portal, float partialTick) {
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
                boolean canCollideWithPortal = canCollideWithPortal(entity, portal, partialTick);
                // use partial tick zero to get the colliding portal before this tick
                if (!canCollideWithPortal) {
                    return;
                }
                
                ((IEEntity) entity).notifyCollidingWithPortal(portal);
            }
        );
    }
    
    @Deprecated
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
                    // partial tick 0 maybe incorrect here
                    // if this method runs in collision ticking
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
        }
        else if (!thisTickStagnate && lastTickStagnate) {
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
        }
        else {
            return portal;
        }
    }
    
    @Environment(EnvType.CLIENT)
    private static PortalLike getCollisionHandlingUnitClient(Portal portal) {
        return PortalGroup.getPortalUnit(portal);
    }
    
    // currently does not support handling the case of colliding with multiple portals at the same time
    // select one (this is a workaround)
    public static Portal chooseCollidingPortalBetweenTwo(
        Entity entity,
        Portal a,
        Portal b
    ) {
        Vec3 velocity = McHelper.getWorldVelocity(entity);
        
        boolean movingTowardsA = velocity.dot(a.getNormal()) < 0;
        boolean movingTowardsB = velocity.dot(b.getNormal()) < 0;
        
        if (movingTowardsA) {
            return a;
        }
        return b;
    }
}
