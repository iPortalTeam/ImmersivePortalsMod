package qouteall.imm_ptl.core.portal;

import com.mojang.math.Quaternion;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import qouteall.imm_ptl.core.IPMcHelper;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.api.PortalAPI;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.MiscHelper;
import qouteall.q_misc_util.my_util.DQuaternion;
import qouteall.q_misc_util.my_util.RotationHelper;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class PortalManipulation {
    public static void setPortalTransformation(
        Portal portal,
        ResourceKey<Level> destDim,
        Vec3 destPos,
        @Nullable Quaternion rotation,
        double scale
    ) {
        portal.dimensionTo = destDim;
        portal.setDestination(destPos);
        portal.rotation = rotation;
        portal.scaling = scale;
        portal.updateCache();
    }
    
    public static void removeConnectedPortals(Portal portal, Consumer<Portal> removalInformer) {
        removeOverlappedPortals(
            portal.level,
            portal.getOriginPos(),
            portal.getNormal().scale(-1),
            p -> Objects.equals(p.specificPlayerId, portal.specificPlayerId),
            removalInformer
        );
        ServerLevel toWorld = MiscHelper.getServer().getLevel(portal.dimensionTo);
        removeOverlappedPortals(
            toWorld,
            portal.getDestPos(),
            portal.transformLocalVecNonScale(portal.getNormal().scale(-1)),
            p -> Objects.equals(p.specificPlayerId, portal.specificPlayerId),
            removalInformer
        );
        removeOverlappedPortals(
            toWorld,
            portal.getDestPos(),
            portal.transformLocalVecNonScale(portal.getNormal()),
            p -> Objects.equals(p.specificPlayerId, portal.specificPlayerId),
            removalInformer
        );
    }
    
    public static Portal completeBiWayPortal(Portal portal, EntityType<? extends Portal> entityType) {
        Portal newPortal = createReversePortal(portal, entityType);
        
        McHelper.spawnServerEntity(newPortal);
        
        return newPortal;
    }
    
    // can also be used in client
    public static <T extends Portal> T createReversePortal(Portal portal, EntityType<T> entityType) {
        Level world = portal.getDestinationWorld();
        
        T newPortal = entityType.create(world);
        newPortal.dimensionTo = portal.level.dimension();
        newPortal.setPos(portal.getDestPos().x, portal.getDestPos().y, portal.getDestPos().z);
        newPortal.setDestination(portal.getOriginPos());
        newPortal.specificPlayerId = portal.specificPlayerId;
        
        newPortal.width = portal.width * portal.scaling;
        newPortal.height = portal.height * portal.scaling;
        newPortal.axisW = portal.axisW;
        newPortal.axisH = portal.axisH.scale(-1);
        
        if (portal.specialShape != null) {
            newPortal.specialShape = new GeometryPortalShape();
            initFlippedShape(newPortal, portal.specialShape, portal.scaling);
        }
        
        newPortal.initCullableRange(
            portal.cullableXStart * portal.scaling,
            portal.cullableXEnd * portal.scaling,
            -portal.cullableYStart * portal.scaling,
            -portal.cullableYEnd * portal.scaling
        );
        
        if (portal.rotation != null) {
            rotatePortalBody(newPortal, portal.rotation);
            
            newPortal.rotation = new Quaternion(portal.rotation);
            newPortal.rotation.conj();
        }
        
        newPortal.scaling = 1.0 / portal.scaling;
        
        copyAdditionalProperties(newPortal, portal);
        
        return newPortal;
    }
    
    public static void rotatePortalBody(Portal portal, Quaternion rotation) {
        portal.axisW = RotationHelper.getRotated(rotation, portal.axisW);
        portal.axisH = RotationHelper.getRotated(rotation, portal.axisH);
    }
    
    public static Portal completeBiFacedPortal(Portal portal, EntityType<Portal> entityType) {
        Portal newPortal = createFlippedPortal(portal, entityType);
        
        McHelper.spawnServerEntity(newPortal);
        
        return newPortal;
    }
    
    public static <T extends Portal> T createFlippedPortal(Portal portal, EntityType<T> entityType) {
        Level world = portal.level;
        T newPortal = entityType.create(world);
        newPortal.dimensionTo = portal.dimensionTo;
        newPortal.setPos(portal.getX(), portal.getY(), portal.getZ());
        newPortal.setDestination(portal.getDestPos());
        newPortal.specificPlayerId = portal.specificPlayerId;
        
        newPortal.width = portal.width;
        newPortal.height = portal.height;
        newPortal.axisW = portal.axisW;
        newPortal.axisH = portal.axisH.scale(-1);
        
        if (portal.specialShape != null) {
            newPortal.specialShape = new GeometryPortalShape();
            initFlippedShape(newPortal, portal.specialShape, 1);
        }
        
        newPortal.initCullableRange(
            portal.cullableXStart,
            portal.cullableXEnd,
            -portal.cullableYStart,
            -portal.cullableYEnd
        );
        
        newPortal.rotation = portal.rotation;
        
        newPortal.scaling = portal.scaling;
        
        copyAdditionalProperties(newPortal, portal);
        
        return newPortal;
    }
    
    //the new portal will not be added into world
    public static Portal copyPortal(Portal portal, EntityType<Portal> entityType) {
        Level world = portal.level;
        Portal newPortal = entityType.create(world);
        newPortal.dimensionTo = portal.dimensionTo;
        newPortal.setPos(portal.getX(), portal.getY(), portal.getZ());
        newPortal.setDestination(portal.getDestPos());
        newPortal.specificPlayerId = portal.specificPlayerId;
        
        newPortal.width = portal.width;
        newPortal.height = portal.height;
        newPortal.axisW = portal.axisW;
        newPortal.axisH = portal.axisH;
        
        newPortal.specialShape = portal.specialShape;
        
        newPortal.initCullableRange(
            portal.cullableXStart,
            portal.cullableXEnd,
            portal.cullableYStart,
            portal.cullableYEnd
        );
        
        newPortal.rotation = portal.rotation;
        
        newPortal.scaling = portal.scaling;
        
        copyAdditionalProperties(newPortal, portal);
        
        return newPortal;
    }
    
    private static void initFlippedShape(Portal newPortal, GeometryPortalShape specialShape, double scale) {
        newPortal.specialShape.triangles = specialShape.triangles.stream()
            .map(triangle -> new GeometryPortalShape.TriangleInPlane(
                triangle.x1 * scale,
                -triangle.y1 * scale,
                triangle.x2 * scale,
                -triangle.y2 * scale,
                triangle.x3 * scale,
                -triangle.y3 * scale
            )).collect(Collectors.toList());
    }
    
    public static void completeBiWayBiFacedPortal(
        Portal portal, Consumer<Portal> removalInformer,
        Consumer<Portal> addingInformer, EntityType<Portal> entityType
    ) {
        removeOverlappedPortals(
            ((ServerLevel) portal.level),
            portal.getOriginPos(),
            portal.getNormal().scale(-1),
            p -> Objects.equals(p.specificPlayerId, portal.specificPlayerId),
            removalInformer
        );
        
        Portal oppositeFacedPortal = completeBiFacedPortal(portal, entityType);
        removeOverlappedPortals(
            MiscHelper.getServer().getLevel(portal.dimensionTo),
            portal.getDestPos(),
            portal.transformLocalVecNonScale(portal.getNormal().scale(-1)),
            p -> Objects.equals(p.specificPlayerId, portal.specificPlayerId),
            removalInformer
        );
        
        Portal r1 = completeBiWayPortal(portal, entityType);
        removeOverlappedPortals(
            MiscHelper.getServer().getLevel(oppositeFacedPortal.dimensionTo),
            oppositeFacedPortal.getDestPos(),
            oppositeFacedPortal.transformLocalVecNonScale(oppositeFacedPortal.getNormal().scale(-1)),
            p -> Objects.equals(p.specificPlayerId, portal.specificPlayerId),
            removalInformer
        );
        
        Portal r2 = completeBiWayPortal(oppositeFacedPortal, entityType);
        addingInformer.accept(oppositeFacedPortal);
        addingInformer.accept(r1);
        addingInformer.accept(r2);
    }
    
    public static void removeOverlappedPortals(
        Level world,
        Vec3 pos,
        Vec3 normal,
        Predicate<Portal> predicate,
        Consumer<Portal> informer
    ) {
        getPortalCluster(world, pos, normal, predicate).forEach(e -> {
            e.remove(Entity.RemovalReason.KILLED);
            informer.accept(e);
        });
    }
    
    public static List<Portal> getPortalCluster(
        Level world,
        Vec3 pos,
        Vec3 normal,
        Predicate<Portal> predicate
    ) {
        return McHelper.findEntitiesByBox(
            Portal.class,
            world,
            new AABB(
                pos.add(0.1, 0.1, 0.1),
                pos.subtract(0.1, 0.1, 0.1)
            ),
            20,
            p -> p.getNormal().dot(normal) > 0.5 && predicate.test(p)
        );
    }
    
    public static <T extends Portal> T createOrthodoxPortal(
        EntityType<T> entityType,
        ServerLevel fromWorld, ServerLevel toWorld,
        Direction facing, AABB portalArea,
        Vec3 destination
    ) {
        T portal = entityType.create(fromWorld);
        
        PortalAPI.setPortalOrthodoxShape(portal, facing, portalArea);
        
        portal.setDestination(destination);
        portal.dimensionTo = toWorld.dimension();
        
        return portal;
    }
    
    public static void copyAdditionalProperties(Portal to, Portal from) {
        copyAdditionalProperties(to, from, true);
    }
    
    public static void copyAdditionalProperties(Portal to, Portal from, boolean includeSpecialProperties) {
        to.teleportable = from.teleportable;
        to.teleportChangesScale = from.teleportChangesScale;
        to.teleportChangesGravity = from.teleportChangesGravity;
        to.specificPlayerId = from.specificPlayerId;
        PortalExtension.get(to).motionAffinity = PortalExtension.get(from).motionAffinity;
        PortalExtension.get(to).adjustPositionAfterTeleport = PortalExtension.get(from).adjustPositionAfterTeleport;
        to.hasCrossPortalCollision = from.hasCrossPortalCollision;
        PortalExtension.get(to).bindCluster = PortalExtension.get(from).bindCluster;
        to.defaultAnimation = from.defaultAnimation.copy();
        to.setIsVisible(from.isVisible());
        
        if (includeSpecialProperties) {
            to.portalTag = from.portalTag;
            to.commandsOnTeleported = from.commandsOnTeleported;
        }
    }
    
    public static void createScaledBoxView(
        ServerLevel areaWorld, AABB area,
        ServerLevel boxWorld, Vec3 boxBottomCenter,
        double scale,
        boolean biWay,
        boolean teleportChangesScale,
        boolean outerFuseView,
        boolean outerRenderingMergable,
        boolean innerRenderingMergable,
        boolean hasCrossPortalCollision
    ) {
        Vec3 viewBoxSize = Helper.getBoxSize(area).scale(1.0 / scale);
        AABB viewBox = Helper.getBoxByBottomPosAndSize(boxBottomCenter, viewBoxSize);
        for (Direction direction : Direction.values()) {
            Portal portal = createOrthodoxPortal(
                Portal.entityType,
                boxWorld, areaWorld,
                direction, Helper.getBoxSurface(viewBox, direction),
                Helper.getBoxSurface(area, direction).getCenter()
            );
            portal.scaling = scale;
            portal.teleportChangesScale = teleportChangesScale;
            portal.fuseView = outerFuseView;
            portal.renderingMergable = outerRenderingMergable;
            portal.hasCrossPortalCollision = hasCrossPortalCollision;
            portal.portalTag = "imm_ptl:scale_box";
            
            McHelper.spawnServerEntity(portal);
            
            if (biWay) {
                Portal reversePortal = createReversePortal(portal, Portal.entityType);
                
                reversePortal.renderingMergable = innerRenderingMergable;
                
                McHelper.spawnServerEntity(reversePortal);
                
            }
        }
    }
    
    /**
     * Places a portal based on {@code entity}'s looking direction. Does not set the portal destination or add it to the
     * world, you will have to do that yourself.
     *
     * @param width  The width of the portal.
     * @param height The height of the portal.
     * @param entity The entity to place this portal as.
     * @return The placed portal, with no destination set.
     * @author LoganDark
     */
    public static Portal placePortal(double width, double height, Entity entity) {
        Vec3 playerLook = entity.getLookAngle();
        
        Tuple<BlockHitResult, List<Portal>> rayTrace =
            IPMcHelper.rayTrace(
                entity.level,
                new ClipContext(
                    entity.getEyePosition(1.0f),
                    entity.getEyePosition(1.0f).add(playerLook.scale(100.0)),
                    ClipContext.Block.OUTLINE,
                    ClipContext.Fluid.NONE,
                    entity
                ),
                true
            );
        
        BlockHitResult hitResult = rayTrace.getA();
        List<Portal> hitPortals = rayTrace.getB();
        
        if (IPMcHelper.hitResultIsMissedOrNull(hitResult)) {
            return null;
        }
        
        for (Portal hitPortal : hitPortals) {
            playerLook = hitPortal.transformLocalVecNonScale(playerLook);
        }
        
        Direction lookingDirection = Helper.getFacingExcludingAxis(
            playerLook,
            hitResult.getDirection().getAxis()
        );
        
        // this should never happen...
        if (lookingDirection == null) {
            return null;
        }
        
        Vec3 axisH = Vec3.atLowerCornerOf(hitResult.getDirection().getNormal());
        Vec3 axisW = axisH.cross(Vec3.atLowerCornerOf(lookingDirection.getOpposite().getNormal()));
        Vec3 pos = Vec3.atCenterOf(hitResult.getBlockPos())
            .add(axisH.scale(0.5 + height / 2));
        
        Level world = hitPortals.isEmpty()
            ? entity.level
            : hitPortals.get(hitPortals.size() - 1).getDestinationWorld();
        
        Portal portal = new Portal(Portal.entityType, world);
        
        portal.setPosRaw(pos.x, pos.y, pos.z);
        
        portal.axisW = axisW;
        portal.axisH = axisH;
        
        portal.width = width;
        portal.height = height;
        
        return portal;
    }
    
    public static DQuaternion getPortalOrientationQuaternion(
        Vec3 axisW, Vec3 axisH
    ) {
        Vec3 normal = axisW.cross(axisH);
        
        return DQuaternion.matrixToQuaternion(axisW, axisH, normal);
    }
    
    public static void setPortalOrientationQuaternion(
        Portal portal, DQuaternion quaternion
    ) {
        portal.setOrientationRotation(quaternion);
    }
    
    public static void adjustRotationToConnect(Portal portalA, Portal portalB) {
        DQuaternion a = PortalAPI.getPortalOrientationQuaternion(portalA);
        DQuaternion b = PortalAPI.getPortalOrientationQuaternion(portalB);
        
        DQuaternion delta = b.hamiltonProduct(a.getConjugated());
        
        DQuaternion flip = DQuaternion.rotationByDegrees(
            portalB.axisH, 180
        );
        DQuaternion aRot = flip.hamiltonProduct(delta);
        
        portalA.setRotationTransformation(aRot.toMcQuaternion());
        portalB.setRotationTransformation(aRot.getConjugated().toMcQuaternion());
        
    }
    
    public static boolean isOtherSideBoxInside(AABB transformedBoundingBox, PortalLike renderingPortal) {
        boolean intersects = Arrays.stream(Helper.eightVerticesOf(transformedBoundingBox))
            .anyMatch(p -> renderingPortal.isInside(p, 0));
        return intersects;
    }
    
    @Nullable
    public static Portal findParallelPortal(Portal portal) {
        return Helper.getFirstNullable(McHelper.findEntitiesRough(
            Portal.class,
            portal.getDestinationWorld(),
            portal.getDestPos(),
            0,
            p1 -> p1.getOriginPos().subtract(portal.getDestPos()).lengthSqr() < 0.01 &&
                p1.getDestPos().subtract(portal.getOriginPos()).lengthSqr() < 0.01 &&
                p1.getNormal().dot(portal.getContentDirection()) < -0.9
        ));
    }
    
    @Nullable
    public static Portal findReversePortal(Portal portal) {
        return Helper.getFirstNullable(McHelper.findEntitiesRough(
            Portal.class,
            portal.getDestinationWorld(),
            portal.getDestPos(),
            0,
            p1 -> p1.getOriginPos().subtract(portal.getDestPos()).lengthSqr() < 0.01 &&
                p1.getDestPos().subtract(portal.getOriginPos()).lengthSqr() < 0.01 &&
                p1.getNormal().dot(portal.getContentDirection()) > 0.9
        ));
    }
    
    @Nullable
    public static Portal findFlippedPortal(Portal portal) {
        return Helper.getFirstNullable(McHelper.findEntitiesRough(
            Portal.class,
            portal.getOriginWorld(),
            portal.getOriginPos(),
            0,
            p1 -> p1.getOriginPos().subtract(portal.getOriginPos()).lengthSqr() < 0.01 &&
                p1.getNormal().dot(portal.getNormal()) < -0.9
        ));
    }
}
