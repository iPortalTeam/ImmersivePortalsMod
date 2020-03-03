package com.qouteall.immersive_portals.portal.global_portals;

import com.qouteall.immersive_portals.McHelper;
import net.minecraft.entity.EntityType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

import java.util.function.Predicate;

public class VerticalConnectingPortal extends GlobalTrackedPortal {
    public static EntityType<VerticalConnectingPortal> entityType;
    
    public static enum ConnectorType {
        ceil, floor
    }
    
    private static Predicate<GlobalTrackedPortal> getPredicate(ConnectorType connectorType) {
        switch (connectorType) {
            case floor:
                return portal -> portal.getY() < 128;
            default:
            case ceil:
                return portal -> portal.getY() > 128;
        }
    }
    
    public VerticalConnectingPortal(
        EntityType<?> entityType_1,
        World world_1
    ) {
        super(entityType_1, world_1);
    }
    
    
    public static void connect(
        DimensionType from,
        ConnectorType connectorType,
        DimensionType to
    ) {
        removeConnectingPortal(connectorType, from);
        
        ServerWorld fromWorld = McHelper.getServer().getWorld(from);
        
        VerticalConnectingPortal connectingPortal = createConnectingPortal(
            fromWorld,
            connectorType,
            McHelper.getServer().getWorld(to)
        );
        
        GlobalPortalStorage storage = GlobalPortalStorage.get(fromWorld);
        
        storage.data.add(connectingPortal);
        
        storage.onDataChanged();
    }
    
    private static VerticalConnectingPortal createConnectingPortal(
        ServerWorld fromWorld,
        ConnectorType connectorType,
        ServerWorld toWorld
    ) {
        VerticalConnectingPortal verticalConnectingPortal = new VerticalConnectingPortal(
            entityType, fromWorld
        );
        
        switch (connectorType) {
            case floor:
                verticalConnectingPortal.updatePosition(0, 0, 0);
                verticalConnectingPortal.destination = new Vec3d(0, 256, 0);
                verticalConnectingPortal.axisW = new Vec3d(0, 0, 1);
                verticalConnectingPortal.axisH = new Vec3d(1, 0, 0);
                break;
            case ceil:
                verticalConnectingPortal.updatePosition(0, 256, 0);
                verticalConnectingPortal.destination = new Vec3d(0, 0, 0);
                verticalConnectingPortal.axisW = new Vec3d(1, 0, 0);
                verticalConnectingPortal.axisH = new Vec3d(0, 0, 1);
                break;
        }
        
        verticalConnectingPortal.dimensionTo = toWorld.dimension.getType();
        verticalConnectingPortal.width = 23333;
        verticalConnectingPortal.height = 23333;
        verticalConnectingPortal.loadFewerChunks = false;
        return verticalConnectingPortal;
    }
    
    public static void removeConnectingPortal(
        ConnectorType connectorType,
        DimensionType dimension
    ) {
        removeConnectingPortal(getPredicate(connectorType), dimension);
    }
    
    private static void removeConnectingPortal(
        Predicate<GlobalTrackedPortal> predicate, DimensionType dimension
    ) {
        ServerWorld endWorld = McHelper.getServer().getWorld(dimension);
        GlobalPortalStorage storage = GlobalPortalStorage.get(endWorld);
    
        storage.data.removeIf(
            portal -> portal instanceof VerticalConnectingPortal && predicate.test(portal)
        );
    
        storage.onDataChanged();
    }
    
    public static VerticalConnectingPortal getConnectingPortal(
        World world, ConnectorType type
    ) {
        return (VerticalConnectingPortal) McHelper.getGlobalPortals(world).stream()
            .filter(getPredicate(type))
            .findFirst().orElse(null);
    }
}
