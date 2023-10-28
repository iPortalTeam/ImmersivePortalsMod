package qouteall.imm_ptl.core.collision;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.compat.GravityChangerInterface;
import qouteall.imm_ptl.core.ducks.IEEntity;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalLike;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.my_util.Plane;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class PortalCollisionHandler {
    private static final int maxCollidingPortals = 6;
    
    public long lastActiveTime;
    public final List<PortalCollisionEntry> portalCollisions = new ArrayList<>();
    
    public boolean isRecentlyCollidingWithPortal(Entity entity) {
        return getTiming(entity) - lastActiveTime < 20;
    }
    
    public void update(Entity entity) {
        portalCollisions.removeIf(p -> {
            if (p.portal.level() != entity.level()) {
                return true;
            }
            
            AABB stretchedBoundingBox = CollisionHelper.getStretchedBoundingBox(entity);
            if (!stretchedBoundingBox.inflate(0.5).intersects(p.portal.getBoundingBox())) {
                return true;
            }
            
            if (Math.abs(getTiming(entity) - p.activeTime) >= 3) {
                return true;
            }
            
            // because that teleportation is based on rendering camera pos,
            // the camera pos is behind this tick pos,
            // this tick pos may go behind the portal before teleportation.
            // use last tick pos to check whether it should collide
            if (!CollisionHelper.canCollideWithPortal(entity, p.portal, 0)) {
                return true;
            }
            
            return false;
        });
    }
    
    private static int getTiming(Entity entity) {
        return entity.tickCount;
    }
    
    public Vec3 handleCollision(
        Entity entity, Vec3 attemptedMove
    ) {
        if (portalCollisions.isEmpty()) {
            return attemptedMove;
        }
        
        entity.level().getProfiler().push("cross_portal_collision");
        
        portalCollisions.sort(
            Comparator.comparingLong((PortalCollisionEntry p) -> p.activeTime).reversed()
        );
        
        Vec3 result = doHandleCollision(entity, attemptedMove, 1, portalCollisions, entity.getBoundingBox());
        
        entity.level().getProfiler().pop();
        
        return result;
    }
    
    private static Vec3 doHandleCollision(
        Entity entity, Vec3 attemptedMove, int portalLayer,
        List<PortalCollisionEntry> portalCollisions, AABB originalBoundingBox
    ) {
        Vec3 currentMove = attemptedMove;
        
        currentMove = handleThisSideMove(
            entity, currentMove,
            originalBoundingBox,
            portalCollisions
        );
        
        for (PortalCollisionEntry portalCollision : portalCollisions) {
            currentMove = handleOtherSideMove(
                entity, currentMove, portalCollision.portal,
                originalBoundingBox, portalLayer
            );
        }
        
        return new Vec3(
            CollisionHelper.fixCoordinateFloatingPointError(attemptedMove.x, currentMove.x),
            CollisionHelper.fixCoordinateFloatingPointError(attemptedMove.y, currentMove.y),
            CollisionHelper.fixCoordinateFloatingPointError(attemptedMove.z, currentMove.z)
        );
    }
    
    private static Vec3 handleOtherSideMove(
        Entity entity,
        Vec3 attemptedMove,
        Portal collidingPortal,
        AABB originalBoundingBox,
        int portalLayer
    ) {
        if (!collidingPortal.getHasCrossPortalCollision()) {
            return attemptedMove;
        }
        
        // limit max recursion layer
        if (portalLayer >= 5) {
            return attemptedMove;
        }
        
        Vec3 transformedAttemptedMove = collidingPortal.transformLocalVec(attemptedMove);
        
        AABB boxOtherSide = CollisionHelper.transformBox(collidingPortal, originalBoundingBox);
        if (boxOtherSide == null) {
            return attemptedMove;
        }
        
        Level destinationWorld = collidingPortal.getDestWorld();
        
        if (!destinationWorld.hasChunkAt(BlockPos.containing(boxOtherSide.getCenter()))) {
            return handleOtherSideChunkNotLoaded(
                entity, attemptedMove, collidingPortal, originalBoundingBox
            );
        }
        
        List<Portal> indirectCollidingPortals = McHelper.findEntitiesByBox(
            Portal.class,
            collidingPortal.getDestinationWorld(),
            boxOtherSide.expandTowards(transformedAttemptedMove),
            IPGlobal.maxNormalPortalRadius,
            p -> CollisionHelper.canCollideWithPortal(entity, p, 0)
                && collidingPortal.isOnDestinationSide(p.getOriginPos(), 0.1)
        );
        
        PortalLike collisionHandlingUnit = CollisionHelper.getCollisionHandlingUnit(collidingPortal);
        Direction transformedGravityDirection = collidingPortal.getTransformedGravityDirection(GravityChangerInterface.invoker.getGravityDirection(entity));
        
        Vec3 collided = transformedAttemptedMove;
        collided = CollisionHelper.handleCollisionWithShapeProcessor(
            entity, boxOtherSide, destinationWorld,
            collided,
            shape -> {
                VoxelShape current = CollisionHelper.clipVoxelShape(
                    shape, collidingPortal.getDestPos(), collidingPortal.getContentDirection()
                );
                
                if (current == null) {
                    return null;
                }
                
                if (!indirectCollidingPortals.isEmpty()) {
                    current = processThisSideCollisionShape(
                        current, indirectCollidingPortals
                    );
                }
                
                return current;
            },
            transformedGravityDirection, collidingPortal.getScale());
        
        if (!indirectCollidingPortals.isEmpty()) {
            for (Portal indirectCollidingPortal : indirectCollidingPortals) {
                collided = handleOtherSideMove(
                    entity, collided,
                    indirectCollidingPortal, boxOtherSide,
                    portalLayer + 1
                );
            }
        }
        
        collided = new Vec3(
            CollisionHelper.fixCoordinateFloatingPointError(transformedAttemptedMove.x, collided.x),
            CollisionHelper.fixCoordinateFloatingPointError(transformedAttemptedMove.y, collided.y),
            CollisionHelper.fixCoordinateFloatingPointError(transformedAttemptedMove.z, collided.z)
        );
        
        Vec3 result = collidingPortal.inverseTransformLocalVec(collided);
        
        return result;
    }
    
    private static Vec3 handleOtherSideChunkNotLoaded(Entity entity, Vec3 attemptedMove, Portal collidingPortal, AABB originalBoundingBox) {
        if (entity instanceof Player && entity.level().isClientSide()) {
            CollisionHelper.informClientStagnant();
        }
        
        // when the other side chunk is not loaded, don't let the player to go into the portal.
        // this works fine for global portals.
        // however, for normal portals, if the portal is not in the same chunk as player, the portal
        // may not load in time and this will not stop player from start falling through solid ground on the other side.
        // When the portal loads, push the bounding box out of portal.
        
        return collidingPortal.getPortalShape().getOffsetForPushingEntityOutOfPortal(
            collidingPortal,
            collidingPortal.getThisSideState(),
            entity, attemptedMove
        );
    }
    
    private static Vec3 handleThisSideMove(
        Entity entity,
        Vec3 attemptedMove,
        AABB originalBoundingBox,
        List<PortalCollisionEntry> portalCollisions
    ) {
        Direction gravity = GravityChangerInterface.invoker.getGravityDirection(entity);
        
        return CollisionHelper.handleCollisionWithShapeProcessor(
            entity, entity.getBoundingBox(), entity.level(),
            attemptedMove,
            shape -> processThisSideCollisionShape(
                shape, Helper.mappedListView(portalCollisions, e -> e.portal)
            ),
            gravity, 1);
    }
    
    @Nullable
    private static VoxelShape processThisSideCollisionShape(
        VoxelShape originalShape, List<Portal> portalCollisions
    ) {
        VoxelShape shape = originalShape;
        
        if (shape.isEmpty()) {
            return shape;
        }
        
        AABB shapeBounds = shape.bounds();
        
        for (int i = 0; i < portalCollisions.size(); i++) {
            Portal portal = portalCollisions.get(i);
            
            Plane clipping = portal.getPortalShape().getOuterClipping(portal.getThisSideState());
            
            if (clipping != null) {
                boolean boxFullyBehindPlane = CollisionHelper.isBoxFullyBehindPlane(
                    clipping.pos(), clipping.normal(), shapeBounds
                );
                
                // it's a workaround for diagonal portals
                // MC does not support not axis-aligned shape collision
                // if the box is not fully behind the plane, keep it
                if (!boxFullyBehindPlane) {
                    continue;
                }
            }
            
            VoxelShape exclusion = portal.getThisSideCollisionExclusion();
            
            if (exclusion == null || exclusion.isEmpty()) {
                continue;
            }
            
            if (Helper.boxContains(exclusion.bounds(), shapeBounds)) {
                // fully excluded
                return null;
            }
            
            shape = Shapes.joinUnoptimized(
                shape,
                exclusion,
                BooleanOp.ONLY_FIRST
            );
            
            if (shape.isEmpty()) {
                return shape;
            }
            
            shapeBounds = shape.bounds();
        }
        
        return shape;
    }
    
    @Nullable
    public AABB getActiveCollisionBox(Entity entity, AABB rawBoundingBox) {
        AABB currentBox = rawBoundingBox;
        
        for (PortalCollisionEntry portalCollision : portalCollisions) {
            Portal portal = portalCollision.portal;
            
            Plane outerClipping = portal.getPortalShape()
                .getOuterClipping(portal.getThisSideState());
            
            if (outerClipping != null) {
                AABB newBox = CollisionHelper.clipBox(
                    currentBox, outerClipping.pos(), outerClipping.normal()
                );
                
                if (newBox == null) {
                    return null;
                }
                
                currentBox = newBox;
            }
        }
        
        return currentBox;
    }
    
    public static void updateCollidingPortalAfterTeleportation(
        Entity entity, Vec3 newEyePos, Vec3 newLastTickEyePos, float partialTicks
    ) {
        ((IEEntity) entity).ip_clearCollidingPortal();
        
        McHelper.findEntitiesByBox(
            Portal.class,
            entity.level(),
            CollisionHelper.getStretchedBoundingBox(entity),
            IPGlobal.maxNormalPortalRadius,
            p -> true
        ).forEach(p -> CollisionHelper.notifyCollidingPortals(p, partialTicks));
        
        // don't tickCollidingPortal() as it only removes collisions
        
        McHelper.setEyePos(entity, newEyePos, newLastTickEyePos);
        McHelper.updateBoundingBox(entity);
    }
    
    public boolean hasCollisionEntry() {
        return !portalCollisions.isEmpty();
    }
    
    public void notifyCollidingWithPortal(Entity entity, Portal portal) {
        if (portalCollisions.size() >= maxCollidingPortals) {
            return;
        }
        
        long timing = getTiming(entity);
        
        int i = Helper.indexOf(portalCollisions, p -> p.portal == portal);
        if (i == -1) {
            portalCollisions.add(new PortalCollisionEntry(portal, timing));
        }
        else {
            portalCollisions.get(i).activeTime = timing;
        }
        
        portal.onCollidingWithEntity(entity);
        
        lastActiveTime = timing;
    }
    
    public List<Portal> getCollidingPortals() {
        return Helper.mappedListView(portalCollisions, p -> p.portal);
    }
    
    @NotNull
    public static Vec3 getOffsetForPushingEntityOutOfPortal(
        Vec3 attemptedMove, Vec3 origin, Vec3 normal, AABB originalBoundingBox
    ) {
        Vec3 innerDirection = normal.scale(-1);
        
        double innerSignedDistance = Arrays
            .stream(Helper.eightVerticesOf(originalBoundingBox))
            .mapToDouble(
                pos -> pos.subtract(origin).dot(normal)
            )
            .min().orElseThrow();
        
        Vec3 attemptedMoveProjectedToInnerDirection =
            innerDirection.scale(innerDirection.dot(attemptedMove));
        
        // subtract the attempted move projection along inner direction
        // to cancel the movement inward
        if (innerSignedDistance < 0) {
            // when the bounding box is already inside portal
            // subtract the extra to push it out
            return attemptedMove
                .add(normal.scale(-innerSignedDistance))
                .subtract(attemptedMoveProjectedToInnerDirection);
        }
        else {
            return attemptedMove
                .subtract(attemptedMoveProjectedToInnerDirection);
        }
    }
}
