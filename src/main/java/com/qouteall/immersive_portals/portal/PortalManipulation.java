package com.qouteall.immersive_portals.portal;

import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import net.minecraft.entity.EntityType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.function.Consumer;
import java.util.stream.Collectors;

public class PortalManipulation {
    public static void removeConnectedPortals(Portal portal, Consumer<Portal> removalInformer) {
        removeOverlappedPortals(
            portal.world,
            portal.getPos(),
            portal.getNormal().multiply(-1),
            removalInformer
        );
        ServerWorld toWorld = McHelper.getServer().getWorld(portal.dimensionTo);
        removeOverlappedPortals(
            toWorld,
            portal.destination,
            portal.transformLocalVec(portal.getNormal().multiply(-1)),
            removalInformer
        );
        removeOverlappedPortals(
            toWorld,
            portal.destination,
            portal.transformLocalVec(portal.getNormal()),
            removalInformer
        );
    }
    
    public static Portal doCompleteBiWayPortal(Portal portal, EntityType<Portal> entityType) {
        ServerWorld world = McHelper.getServer().getWorld(portal.dimensionTo);
        
        Portal newPortal = entityType.create(world);
        newPortal.dimensionTo = portal.dimension;
        newPortal.updatePosition(portal.destination.x, portal.destination.y, portal.destination.z);
        newPortal.destination = portal.getPos();
        newPortal.loadFewerChunks = portal.loadFewerChunks;
        newPortal.specificPlayer = portal.specificPlayer;
        
        newPortal.width = portal.width;
        newPortal.height = portal.height;
        newPortal.axisW = portal.axisW;
        newPortal.axisH = portal.axisH.multiply(-1);
        
        if (portal.specialShape != null) {
            newPortal.specialShape = new GeometryPortalShape();
            initFlippedShape(newPortal, portal.specialShape);
        }
        
        newPortal.initCullableRange(
            portal.cullableXStart,
            portal.cullableXEnd,
            -portal.cullableYStart,
            -portal.cullableYEnd
        );
        
        if (portal.rotation != null) {
            newPortal.axisW = Helper.getRotated(portal.rotation, newPortal.axisW);
            newPortal.axisH = Helper.getRotated(portal.rotation, newPortal.axisH);
            
            newPortal.rotation = portal.rotation.copy();
            newPortal.rotation.conjugate();
        }
        
        newPortal.motionAffinity = portal.motionAffinity;
        
        world.spawnEntity(newPortal);
        
        return newPortal;
    }
    
    public static Portal doCompleteBiFacedPortal(Portal portal, EntityType<Portal> entityType) {
        ServerWorld world = (ServerWorld) portal.world;
        Portal newPortal = entityType.create(world);
        newPortal.dimensionTo = portal.dimensionTo;
        newPortal.updatePosition(portal.getX(), portal.getY(), portal.getZ());
        newPortal.destination = portal.destination;
        newPortal.loadFewerChunks = portal.loadFewerChunks;
        newPortal.specificPlayer = portal.specificPlayer;
        
        newPortal.width = portal.width;
        newPortal.height = portal.height;
        newPortal.axisW = portal.axisW;
        newPortal.axisH = portal.axisH.multiply(-1);
        
        if (portal.specialShape != null) {
            newPortal.specialShape = new GeometryPortalShape();
            initFlippedShape(newPortal, portal.specialShape);
        }
        
        newPortal.initCullableRange(
            portal.cullableXStart,
            portal.cullableXEnd,
            -portal.cullableYStart,
            -portal.cullableYEnd
        );
        
        newPortal.rotation = portal.rotation;
        
        newPortal.motionAffinity = portal.motionAffinity;
        
        world.spawnEntity(newPortal);
        
        return newPortal;
    }
    
    private static void initFlippedShape(Portal newPortal, GeometryPortalShape specialShape) {
        newPortal.specialShape.triangles = specialShape.triangles.stream()
            .map(triangle -> new GeometryPortalShape.TriangleInPlane(
                triangle.x1,
                -triangle.y1,
                triangle.x2,
                -triangle.y2,
                triangle.x3,
                -triangle.y3
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
            removalInformer
        );
        
        Portal oppositeFacedPortal = doCompleteBiFacedPortal(portal, entityType);
        removeOverlappedPortals(
            McHelper.getServer().getWorld(portal.dimensionTo),
            portal.destination,
            portal.transformLocalVec(portal.getNormal().multiply(-1)),
            removalInformer
        );
        
        Portal r1 = doCompleteBiWayPortal(portal, entityType);
        removeOverlappedPortals(
            McHelper.getServer().getWorld(oppositeFacedPortal.dimensionTo),
            oppositeFacedPortal.destination,
            oppositeFacedPortal.transformLocalVec(oppositeFacedPortal.getNormal().multiply(-1)),
            removalInformer
        );
        
        Portal r2 = doCompleteBiWayPortal(oppositeFacedPortal, entityType);
        addingInformer.accept(oppositeFacedPortal);
        addingInformer.accept(r1);
        addingInformer.accept(r2);
    }
    
    public static void removeOverlappedPortals(
        World world,
        Vec3d pos,
        Vec3d normal,
        Consumer<Portal> informer
    ) {
        world.getEntities(
            Portal.class,
            new Box(
                pos.add(0.5, 0.5, 0.5),
                pos.subtract(0.5, 0.5, 0.5)
            ),
            p -> p.getNormal().dotProduct(normal) > 0.5
        ).forEach(e -> {
            e.remove();
            informer.accept(e);
        });
    }
}
