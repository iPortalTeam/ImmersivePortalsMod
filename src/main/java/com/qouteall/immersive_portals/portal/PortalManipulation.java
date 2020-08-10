package com.qouteall.immersive_portals.portal;

import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.my_util.RotationHelper;
import net.minecraft.entity.EntityType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Pair;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class PortalManipulation {
    public static void removeConnectedPortals(Portal portal, Consumer<Portal> removalInformer) {
        removeOverlappedPortals(
            portal.world,
            portal.getPos(),
            portal.getNormal().multiply(-1),
            p -> Objects.equals(p.specificPlayerId, portal.specificPlayerId),
            removalInformer
        );
        ServerWorld toWorld = McHelper.getServer().getWorld(portal.dimensionTo);
        removeOverlappedPortals(
            toWorld,
            portal.destination,
            portal.transformLocalVecNonScale(portal.getNormal().multiply(-1)),
            p -> Objects.equals(p.specificPlayerId, portal.specificPlayerId),
            removalInformer
        );
        removeOverlappedPortals(
            toWorld,
            portal.destination,
            portal.transformLocalVecNonScale(portal.getNormal()),
            p -> Objects.equals(p.specificPlayerId, portal.specificPlayerId),
            removalInformer
        );
    }
    
    public static Portal completeBiWayPortal(Portal portal, EntityType<? extends Portal> entityType) {
        Portal newPortal = createReversePortal(portal, entityType);
    
        McHelper.spawnServerEntityToUnloadedArea(newPortal);
        
        return newPortal;
    }
    
    public static <T extends Portal> T createReversePortal(Portal portal, EntityType<T> entityType) {
        ServerWorld world = McHelper.getServer().getWorld(portal.dimensionTo);
        
        T newPortal = entityType.create(world);
        newPortal.dimensionTo = portal.world.getRegistryKey();
        newPortal.updatePosition(portal.destination.x, portal.destination.y, portal.destination.z);
        newPortal.destination = portal.getPos();
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
    
    public static void rotatePortalBody(Portal newPortal, Quaternion rotation) {
        newPortal.axisW = RotationHelper.getRotated(rotation, newPortal.axisW);
        newPortal.axisH = RotationHelper.getRotated(rotation, newPortal.axisH);
    }
    
    public static Portal completeBiFacedPortal(Portal portal, EntityType<Portal> entityType) {
        Portal newPortal = createFlippedPortal(portal, entityType);
    
        McHelper.spawnServerEntityToUnloadedArea(newPortal);
        
        return newPortal;
    }
    
    public static <T extends Portal> T createFlippedPortal(Portal portal, EntityType<T> entityType) {
        ServerWorld world = (ServerWorld) portal.world;
        T newPortal = entityType.create(world);
        newPortal.dimensionTo = portal.dimensionTo;
        newPortal.updatePosition(portal.getX(), portal.getY(), portal.getZ());
        newPortal.destination = portal.destination;
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
        ServerWorld world = (ServerWorld) portal.world;
        Portal newPortal = entityType.create(world);
        newPortal.dimensionTo = portal.dimensionTo;
        newPortal.updatePosition(portal.getX(), portal.getY(), portal.getZ());
        newPortal.destination = portal.destination;
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
            portal.getPos(),
            portal.getNormal().multiply(-1),
            p -> Objects.equals(p.specificPlayerId, portal.specificPlayerId),
            removalInformer
        );
        
        Portal oppositeFacedPortal = completeBiFacedPortal(portal, entityType);
        removeOverlappedPortals(
            McHelper.getServer().getWorld(portal.dimensionTo),
            portal.destination,
            portal.transformLocalVecNonScale(portal.getNormal().multiply(-1)),
            p -> Objects.equals(p.specificPlayerId, portal.specificPlayerId),
            removalInformer
        );
        
        Portal r1 = completeBiWayPortal(portal, entityType);
        removeOverlappedPortals(
            McHelper.getServer().getWorld(oppositeFacedPortal.dimensionTo),
            oppositeFacedPortal.destination,
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
            e.remove();
            informer.accept(e);
        });
    }
    
    public static List<Portal> getPortalClutter(
        World world,
        Vec3d pos,
        Vec3d normal,
        Predicate<Portal> predicate
    ) {
        return world.getEntities(
            Portal.class,
            new Box(
                pos.add(0.5, 0.5, 0.5),
                pos.subtract(0.5, 0.5, 0.5)
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
        
        Pair<Direction, Direction> directions = Helper.getPerpendicularDirections(facing);
        
        Vec3d areaSize = Helper.getBoxSize(portalArea);
        
        Box boxSurface = Helper.getBoxSurface(portalArea, facing);
        Vec3d center = boxSurface.getCenter();
        portal.updatePosition(center.x, center.y, center.z);
        portal.destination = destination;
        
        portal.axisW = Vec3d.of(directions.getLeft().getVector());
        portal.axisH = Vec3d.of(directions.getRight().getVector());
        portal.width = Helper.getCoordinate(areaSize, directions.getLeft().getAxis());
        portal.height = Helper.getCoordinate(areaSize, directions.getRight().getAxis());
        
        portal.dimensionTo = toWorld.getRegistryKey();
        
        return portal;
    }
    
    public static void copyAdditionalProperties(Portal to, Portal from) {
        to.extension.motionAffinity = from.extension.motionAffinity;
        to.teleportable = from.teleportable;
        to.teleportChangesScale = from.teleportChangesScale;
        to.specificPlayerId = from.specificPlayerId;
        to.extension.adjustPositionAfterTeleport = from.extension.adjustPositionAfterTeleport;
    }
}
