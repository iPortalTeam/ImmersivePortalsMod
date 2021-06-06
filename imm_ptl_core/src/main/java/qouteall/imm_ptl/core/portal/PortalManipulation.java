package qouteall.imm_ptl.core.portal;

import qouteall.imm_ptl.core.Helper;
import qouteall.imm_ptl.core.IPMcHelper;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.api.PortalAPI;
import qouteall.q_misc_util.my_util.DQuaternion;
import qouteall.q_misc_util.my_util.RotationHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Pair;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

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
        RegistryKey<World> destDim,
        Vec3d destPos,
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
            portal.world,
            portal.getOriginPos(),
            portal.getNormal().multiply(-1),
            p -> Objects.equals(p.specificPlayerId, portal.specificPlayerId),
            removalInformer
        );
        ServerWorld toWorld = McHelper.getServer().getWorld(portal.dimensionTo);
        removeOverlappedPortals(
            toWorld,
            portal.getDestPos(),
            portal.transformLocalVecNonScale(portal.getNormal().multiply(-1)),
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
        World world = portal.getDestinationWorld();
        
        T newPortal = entityType.create(world);
        newPortal.dimensionTo = portal.world.getRegistryKey();
        newPortal.setPosition(portal.getDestPos().x, portal.getDestPos().y, portal.getDestPos().z);
        newPortal.setDestination(portal.getOriginPos());
        newPortal.specificPlayerId = portal.specificPlayerId;
        
        newPortal.width = portal.width * portal.scaling;
        newPortal.height = portal.height * portal.scaling;
        newPortal.axisW = portal.axisW;
        newPortal.axisH = portal.axisH.multiply(-1);
        
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
            newPortal.rotation.conjugate();
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
        World world = portal.world;
        T newPortal = entityType.create(world);
        newPortal.dimensionTo = portal.dimensionTo;
        newPortal.setPosition(portal.getX(), portal.getY(), portal.getZ());
        newPortal.setDestination(portal.getDestPos());
        newPortal.specificPlayerId = portal.specificPlayerId;
        
        newPortal.width = portal.width;
        newPortal.height = portal.height;
        newPortal.axisW = portal.axisW;
        newPortal.axisH = portal.axisH.multiply(-1);
        
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
        World world = portal.world;
        Portal newPortal = entityType.create(world);
        newPortal.dimensionTo = portal.dimensionTo;
        newPortal.setPosition(portal.getX(), portal.getY(), portal.getZ());
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
            ((ServerWorld) portal.world),
            portal.getOriginPos(),
            portal.getNormal().multiply(-1),
            p -> Objects.equals(p.specificPlayerId, portal.specificPlayerId),
            removalInformer
        );
        
        Portal oppositeFacedPortal = completeBiFacedPortal(portal, entityType);
        removeOverlappedPortals(
            McHelper.getServer().getWorld(portal.dimensionTo),
            portal.getDestPos(),
            portal.transformLocalVecNonScale(portal.getNormal().multiply(-1)),
            p -> Objects.equals(p.specificPlayerId, portal.specificPlayerId),
            removalInformer
        );
        
        Portal r1 = completeBiWayPortal(portal, entityType);
        removeOverlappedPortals(
            McHelper.getServer().getWorld(oppositeFacedPortal.dimensionTo),
            oppositeFacedPortal.getDestPos(),
            oppositeFacedPortal.transformLocalVecNonScale(oppositeFacedPortal.getNormal().multiply(-1)),
            p -> Objects.equals(p.specificPlayerId, portal.specificPlayerId),
            removalInformer
        );
        
        Portal r2 = completeBiWayPortal(oppositeFacedPortal, entityType);
        addingInformer.accept(oppositeFacedPortal);
        addingInformer.accept(r1);
        addingInformer.accept(r2);
    }
    
    public static void removeOverlappedPortals(
        World world,
        Vec3d pos,
        Vec3d normal,
        Predicate<Portal> predicate,
        Consumer<Portal> informer
    ) {
        getPortalClutter(world, pos, normal, predicate).forEach(e -> {
            e.remove(Entity.RemovalReason.KILLED);
            informer.accept(e);
        });
    }
    
    public static List<Portal> getPortalClutter(
        World world,
        Vec3d pos,
        Vec3d normal,
        Predicate<Portal> predicate
    ) {
        return world.getEntitiesByClass(
            Portal.class,
            new Box(
                pos.add(0.1, 0.1, 0.1),
                pos.subtract(0.1, 0.1, 0.1)
            ),
            p -> p.getNormal().dotProduct(normal) > 0.5 && predicate.test(p)
        );
    }
    
    public static <T extends Portal> T createOrthodoxPortal(
        EntityType<T> entityType,
        ServerWorld fromWorld, ServerWorld toWorld,
        Direction facing, Box portalArea,
        Vec3d destination
    ) {
        T portal = entityType.create(fromWorld);
        
        PortalAPI.setPortalOrthodoxShape(portal, facing, portalArea);
        
        portal.setDestination(destination);
        portal.dimensionTo = toWorld.getRegistryKey();
        
        return portal;
    }
    
    public static void copyAdditionalProperties(Portal to, Portal from) {
        to.teleportable = from.teleportable;
        to.teleportChangesScale = from.teleportChangesScale;
        to.specificPlayerId = from.specificPlayerId;
        PortalExtension.get(to).motionAffinity = PortalExtension.get(from).motionAffinity;
        PortalExtension.get(to).adjustPositionAfterTeleport = PortalExtension.get(from).adjustPositionAfterTeleport;
        to.portalTag = from.portalTag;
        to.hasCrossPortalCollision = from.hasCrossPortalCollision;
        to.commandsOnTeleported = from.commandsOnTeleported;
    }
    
    public static void createScaledBoxView(
        ServerWorld areaWorld, Box area,
        ServerWorld boxWorld, Vec3d boxBottomCenter,
        double scale,
        boolean biWay,
        boolean teleportChangesScale,
        boolean outerFuseView,
        boolean outerRenderingMergable,
        boolean innerRenderingMergable,
        boolean hasCrossPortalCollision
    ) {
        Vec3d viewBoxSize = Helper.getBoxSize(area).multiply(1.0 / scale);
        Box viewBox = Helper.getBoxByBottomPosAndSize(boxBottomCenter, viewBoxSize);
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
            PortalExtension.get(portal).adjustPositionAfterTeleport = true;
            
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
        Vec3d playerLook = entity.getRotationVector();
        
        Pair<BlockHitResult, List<Portal>> rayTrace =
            IPMcHelper.rayTrace(
                entity.world,
                new RaycastContext(
                    entity.getCameraPosVec(1.0f),
                    entity.getCameraPosVec(1.0f).add(playerLook.multiply(100.0)),
                    RaycastContext.ShapeType.OUTLINE,
                    RaycastContext.FluidHandling.NONE,
                    entity
                ),
                true
            );
        
        BlockHitResult hitResult = rayTrace.getLeft();
        List<Portal> hitPortals = rayTrace.getRight();
        
        if (IPMcHelper.hitResultIsMissedOrNull(hitResult)) {
            return null;
        }
        
        for (Portal hitPortal : hitPortals) {
            playerLook = hitPortal.transformLocalVecNonScale(playerLook);
        }
        
        Direction lookingDirection = Helper.getFacingExcludingAxis(
            playerLook,
            hitResult.getSide().getAxis()
        );
        
        // this should never happen...
        if (lookingDirection == null) {
            return null;
        }
        
        Vec3d axisH = Vec3d.of(hitResult.getSide().getVector());
        Vec3d axisW = axisH.crossProduct(Vec3d.of(lookingDirection.getOpposite().getVector()));
        Vec3d pos = Vec3d.ofCenter(hitResult.getBlockPos())
            .add(axisH.multiply(0.5 + height / 2));
        
        World world = hitPortals.isEmpty()
            ? entity.world
            : hitPortals.get(hitPortals.size() - 1).getDestinationWorld();
        
        Portal portal = new Portal(Portal.entityType, world);
        
        portal.setPos(pos.x, pos.y, pos.z);
        
        portal.axisW = axisW;
        portal.axisH = axisH;
        
        portal.width = width;
        portal.height = height;
        
        return portal;
    }
    
    public static DQuaternion getPortalOrientationQuaternion(
        Vec3d axisW, Vec3d axisH
    ) {
        Vec3d normal = axisW.crossProduct(axisH);
        
        return DQuaternion.matrixToQuaternion(axisW, axisH, normal);
    }
    
    public static void setPortalOrientationQuaternion(
        Portal portal, DQuaternion quaternion
    ) {
        portal.setOrientation(
            quaternion.rotate(new Vec3d(1, 0, 0)),
            quaternion.rotate(new Vec3d(0, 1, 0))
        );
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
    
    public static boolean isOtherSideBoxInside(Box transformedBoundingBox, PortalLike renderingPortal) {
        boolean intersects = Arrays.stream(Helper.eightVerticesOf(transformedBoundingBox))
            .anyMatch(p -> renderingPortal.isInside(p, 0));
        return intersects;
    }
}
