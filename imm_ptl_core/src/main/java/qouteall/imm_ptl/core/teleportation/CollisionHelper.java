package qouteall.imm_ptl.core.teleportation;

import com.google.common.collect.ImmutableList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.compat.PehkuiInterface;
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
import java.util.function.BiFunction;
import java.util.function.Function;

public class CollisionHelper {
    
    private static final LimitedLogger limitedLogger = new LimitedLogger(20);
    
    /**
     * cut a box with a plane.
     * the facing that normal points to will be remained.
     * return null for empty box.
     */
    @Nullable
    public static AABB clipBox(AABB box, Vec3 planePos, Vec3 planeNormal) {
        
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
            return portal.isInFrontOfPortal(cameraPosVec);
        }
        return false;
    }
    
    public static double absMin(double a, double b) {
        return Math.abs(a) < Math.abs(b) ? a : b;
    }
    
    // floating point deviation may cause collision issues
    public static double fixCoordinateFloatingPointError(
        double attemptedMove, double result
    ) {
        //rotation may cause a free move to reduce a little bit and the game think that it's collided
        if (Math.abs(attemptedMove - result) < 0.001) {
            return attemptedMove;
        }
        
        //0 may become 0.0000001 after rotation. avoid falling through floor
        if (Math.abs(result) < 0.0001) {
            return 0;
        }
        
        return result;
    }
    
    @Nullable
    public static VoxelShape clipVoxelShape(VoxelShape shape, Vec3 clippingPlanePos, Vec3 clippingPlaneNormal) {
        if (shape.isEmpty()) {
            return null;
        }
        
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
    }
    
    // only for reference
    private static Vec3 refHandleCollisionWithShapeProcessor(Entity entity, Vec3 attemptedMove, Function<VoxelShape, VoxelShape> filter) {
        AABB boundingBox = entity.getBoundingBox();
        List<VoxelShape> entityCollisions = entity.level.getEntityCollisions(entity, boundingBox.expandTowards(attemptedMove));
        
        // introduce a helper func to reduce argument count
        BiFunction<Vec3, AABB, Vec3> collisionFunc = (attempt, bb) ->
            collideBoundingBox(entity, attempt, bb, entity.level, entityCollisions, filter);
        
        // firstly do a normal collision regardless of stepping
        Vec3 collidedMovement = attemptedMove.lengthSqr() == 0.0D ? attemptedMove :
            collisionFunc.apply(attemptedMove, boundingBox);
        boolean collideX = attemptedMove.x != collidedMovement.x;
        boolean collideY = attemptedMove.y != collidedMovement.y;
        boolean collideZ = attemptedMove.z != collidedMovement.z;
        boolean collidesWithFloor = collideY && attemptedMove.y < 0.0D;
        boolean touchGround = entity.isOnGround() || collidesWithFloor;
        boolean collidesHorizontally = collideX || collideZ;
        float maxUpStep = entity.maxUpStep();
        if (maxUpStep > 0.0F && touchGround && collidesHorizontally) {
            // the entity is touching ground and has horizontal collision now
            // try to directly move to stepped position, make it approach the stair
            Vec3 stepping = collisionFunc.apply(
                new Vec3(attemptedMove.x, (double) maxUpStep, attemptedMove.z),
                boundingBox
            );
            // try to move up in step height with expanded box
            Vec3 verticalStep = collisionFunc.apply(
                new Vec3(0.0D, (double) maxUpStep, 0.0D),
                boundingBox.expandTowards(attemptedMove.x, 0.0D, attemptedMove.z)
            );
            if (verticalStep.y < (double) maxUpStep) {
                // try to move horizontally after moving up
                Vec3 horizontalMoveAfterVerticalStepping = collisionFunc.apply(
                    new Vec3(attemptedMove.x, 0.0D, attemptedMove.z),
                    boundingBox.move(verticalStep)
                ).add(verticalStep);
                // if it's further than directly stepping, use that as the stepped movement
                if (horizontalMoveAfterVerticalStepping.horizontalDistanceSqr() > stepping.horizontalDistanceSqr()) {
                    stepping = horizontalMoveAfterVerticalStepping;
                }
            }
            
            if (stepping.horizontalDistanceSqr() > collidedMovement.horizontalDistanceSqr()) {
                // in the stepped position, move down (because the max step height may be higher than slab height)
                Vec3 movingDown = collisionFunc.apply(
                    new Vec3(0.0D, -stepping.y + attemptedMove.y, 0.0D),
                    boundingBox.move(stepping)
                );
                return stepping.add(movingDown);
            }
        }
        
        return collidedMovement;
    }
    
    /**
     * Vanilla copy {@link Entity#collide(Vec3)}
     * But filters collisions behind the clipping plane and handles stepping with rotated gravity.
     */
    @IPVanillaCopy
    public static Vec3 handleCollisionWithShapeProcessor(
        Entity entity, Vec3 attemptedMove, Function<VoxelShape, VoxelShape> filter,
        Direction gravity, double steppingScale
    ) {
        Direction jumpDirection = gravity.getOpposite();
        Direction.Axis gravityAxis = gravity.getAxis();
        
        AABB boundingBox = entity.getBoundingBox();
        List<VoxelShape> entityCollisions = entity.level.getEntityCollisions(entity, boundingBox.expandTowards(attemptedMove));
        
        // introduce a helper func to reduce argument count
        BiFunction<Vec3, AABB, Vec3> collisionFunc = (attempt, bb) ->
            collideBoundingBox(entity, attempt, bb, entity.level, entityCollisions, filter);
        
        // firstly do a normal collision regardless of stepping
        Vec3 collidedMovement = attemptedMove.lengthSqr() == 0.0D ? attemptedMove :
            collisionFunc.apply(attemptedMove, boundingBox);
        Vec3 collisionDelta = attemptedMove.subtract(collidedMovement);
        boolean collidesOnGravityAxis = Helper.getCoordinate(collisionDelta, gravityAxis) != 0;
        boolean attemptToMoveAlongGravity = Helper.getSignedCoordinate(attemptedMove, gravity) > 0;
        boolean collidesWithFloor = collidesOnGravityAxis && attemptToMoveAlongGravity;
        boolean touchGround = entity.isOnGround() || collidesWithFloor;
        boolean collidesHorizontally = movesOnNonGravityAxis(collisionDelta, gravityAxis);
        float maxUpStep = entity.maxUpStep()
            * PehkuiInterface.invoker.getBaseScale(entity);
        if (steppingScale > 1) {
            maxUpStep *= steppingScale;
        }
        if (maxUpStep > 0.0F && touchGround && collidesHorizontally) {
            // the entity is touching ground and has horizontal collision now
            // try to directly move to stepped position, make it approach the stair
            Vec3 stepping = collisionFunc.apply(
                Helper.putSignedCoordinate(attemptedMove, jumpDirection, maxUpStep),
                boundingBox
            );
            // try to move up in step height with expanded box
            Vec3 expandVec = Helper.putSignedCoordinate(attemptedMove, gravity, 0);
            Vec3 verticalStep = collisionFunc.apply(
                Helper.putSignedCoordinate(Vec3.ZERO, jumpDirection, maxUpStep),
                boundingBox.expandTowards(expandVec)
            );
            // add 0.001 because of floating point error
            if (Helper.getSignedCoordinate(verticalStep, jumpDirection) < (double) maxUpStep + 0.001) {
                // try to move horizontally after moving up
                Vec3 horizontalMoveAfterVerticalStepping = collisionFunc.apply(
                    expandVec,
                    boundingBox.move(verticalStep)
                ).add(verticalStep);
                // if it's further than directly stepping, use that as the stepped movement
                if (Helper.getDistanceSqrOnAxisPlane(horizontalMoveAfterVerticalStepping, gravityAxis) >
                    Helper.getDistanceSqrOnAxisPlane(stepping, gravityAxis)
                ) {
                    stepping = horizontalMoveAfterVerticalStepping;
                }
            }
            
            if (Helper.getDistanceSqrOnAxisPlane(stepping, gravityAxis) >
                Helper.getDistanceSqrOnAxisPlane(collidedMovement, gravityAxis)
            ) {
                double steppingVerticalLen = Helper.getSignedCoordinate(stepping, jumpDirection);
                double attemptMoveVerticalLen = Helper.getSignedCoordinate(attemptedMove, jumpDirection);
                
                // in the stepped position, move down (because the max step height may be higher than slab height)
                Vec3 movingDown = collisionFunc.apply(
                    Helper.putSignedCoordinate(
                        Vec3.ZERO, jumpDirection,
                        -steppingVerticalLen + attemptMoveVerticalLen
                    ),
                    boundingBox.move(stepping)
                );
                return stepping.add(movingDown);
            }
        }
        
        return collidedMovement;
    }
    
    public static boolean movesOnNonGravityAxis(Vec3 vec, Direction.Axis gravityAxis) {
        return switch (gravityAxis) {
            case X -> vec.y != 0 || vec.z != 0;
            case Y -> vec.x != 0 || vec.z != 0;
            case Z -> vec.x != 0 || vec.y != 0;
        };
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
    
    public static AABB transformBox(PortalLike portal, AABB originalBox) {
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
    
    public static void updateCollidingPortalForWorld(Level world, float tickDelta) {
        world.getProfiler().push("update_colliding_portal");
        
        List<Portal> globalPortals = GlobalPortalStorage.getGlobalPortals(world);
        Iterable<Entity> worldEntityList = McHelper.getWorldEntityList(world);
        
        for (Entity entity : worldEntityList) {
            if (entity instanceof Portal portal) {
                // the colliding portal update must happen after all entities finishes ticking,
                // because the entity moves during ticking.
                CollisionHelper.notifyCollidingPortals(portal, tickDelta);
            }
            else {
                AABB entityBoundingBoxStretched = getStretchedBoundingBox(entity);
                for (Portal globalPortal : globalPortals) {
                    AABB globalPortalBoundingBox = globalPortal.getBoundingBox();
                    if (entityBoundingBoxStretched.intersects(globalPortalBoundingBox)) {
                        if (canCollideWithPortal(entity, globalPortal, tickDelta)) {
                            ((IEEntity) entity).ip_notifyCollidingWithPortal(globalPortal);
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
                updateCollidingPortalForWorld(world, 0);
            }
        });
    }
    
    @Environment(EnvType.CLIENT)
    public static void initClient() {
        IPGlobal.postClientTickSignal.connect(CollisionHelper::tickClient);
    }
    
    @Environment(EnvType.CLIENT)
    public static void tickClient() {
        updateClientCollidingStatus();
        
        updateClientStagnateStatus();
    }
    
    @Environment(EnvType.CLIENT)
    private static void updateClientCollidingStatus() {
        if (ClientWorldLoader.getIsInitialized()) {
            for (ClientLevel world : ClientWorldLoader.getClientWorlds()) {
                updateCollidingPortalForWorld(world, 0);
            }
        }
    }
    
    /**
     * Note that there are 3 kinds of portals in the aspect of collision:
     * 1. non-teleportable portal, doesn't affect collision
     * 2. teleportable but non-hasCrossPortalCollision, only affect this-side collision
     * 3. teleportable and hasCrossPortalCollision
     */
    public static void notifyCollidingPortals(Portal portal, float partialTick) {
        if (!portal.teleportable) {
            return;
        }
        
        AABB portalBoundingBox = portal.getBoundingBox();
        
        McHelper.foreachEntitiesByBoxApproximateRegions(
            Entity.class, portal.level,
            portalBoundingBox, 8,
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
                
                ((IEEntity) entity).ip_notifyCollidingWithPortal(portal);
            }
        );
    }
    
    public static AABB getStretchedBoundingBox(Entity entity) {
        // normal colliding portal update lags 1 tick before collision calculation
        // the velocity updates later after updating colliding portal
        // expand the velocity to avoid not collide with portal in time
        Vec3 backwardExpand = McHelper.lastTickPosOf(entity).subtract(entity.position());
        Vec3 forwardExpand = McHelper.getWorldVelocity(entity);
        AABB box = entity.getBoundingBox()
            .expandTowards(forwardExpand.scale(1.2))
            .expandTowards(backwardExpand);
        
        // when the scale is big, the entity could move quickly abruptly
        float scale = PehkuiInterface.invoker.getBaseScale(entity);
        if (scale > 4) {
            box = box.inflate(scale);
        }
        
        return box;
    }
    
    private static boolean thisTickStagnate = false;
    private static boolean lastTickStagnate = false;
    
    @Environment(EnvType.CLIENT)
    public static void informClientStagnant() {
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
    public static PortalLike getCollisionHandlingUnitClient(Portal portal) {
        return PortalGroup.getPortalUnit(portal);
    }
    
    // currently does not support handling the case of colliding with multiple portals at the same time
    // select one (this is a workaround)
    @Deprecated
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
    
    @Nullable
    public static AABB getTotalBlockCollisionBox(Entity entity, AABB box, Function<VoxelShape, VoxelShape> shapeFilter) {
        Iterable<VoxelShape> collisions = entity.level.getBlockCollisions(entity, box);
        
        AABB collisionUnion = null;
        for (VoxelShape c : collisions) {
            VoxelShape currCollision = shapeFilter.apply(c);
            if (currCollision != null && !currCollision.isEmpty()) {
                AABB collisionBoundingBox = currCollision.bounds();
                if (collisionUnion == null) {
                    collisionUnion = collisionBoundingBox;
                }
                else {
                    collisionUnion = collisionUnion.minmax(collisionBoundingBox);
                }
            }
        }
        return collisionUnion;
    }
    
}
