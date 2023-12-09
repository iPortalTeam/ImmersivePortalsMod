package qouteall.imm_ptl.core.portal.global_portals;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalExtension;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.MiscHelper;
import qouteall.q_misc_util.my_util.DQuaternion;

import java.util.function.Predicate;

public class VerticalConnectingPortal extends GlobalTrackedPortal {
    public static final EntityType<VerticalConnectingPortal> entityType =
        createPortalEntityType(VerticalConnectingPortal::new);
    
    public static enum ConnectorType {
        ceil, floor
    }
    
    private static Predicate<Portal> getPredicate(ConnectorType connectorType) {
        switch (connectorType) {
            case floor:
                return portal -> portal instanceof VerticalConnectingPortal && portal.getNormal().y > 0;
            default:
            case ceil:
                return portal -> portal instanceof VerticalConnectingPortal && portal.getNormal().y < 0;
        }
    }
    
    public VerticalConnectingPortal(EntityType<?> entityType, Level world) {
        super(entityType, world);
    }
    
    public static void connect(
        ResourceKey<Level> from,
        ConnectorType connectorType,
        ResourceKey<Level> to
    ) {
        connect(from, connectorType, to, false);
    }
    
    public static void connect(
        ResourceKey<Level> from,
        ConnectorType connectorType,
        ResourceKey<Level> to,
        boolean respectSpaceRatio
    ) {
        removeConnectingPortal(connectorType, from);
        
        ServerLevel fromWorld = MiscHelper.getServer().getLevel(from);
        ServerLevel toWorld = MiscHelper.getServer().getLevel(to);
        
        VerticalConnectingPortal connectingPortal = createConnectingPortal(
            fromWorld,
            connectorType,
            toWorld,
            respectSpaceRatio ?
                fromWorld.dimensionType().coordinateScale() /
                    toWorld.dimensionType().coordinateScale()
                : 1.0,
            false,
            0,
            McHelper.getMinY(fromWorld), McHelper.getMaxContentYExclusive(fromWorld),
            McHelper.getMinY(toWorld),
            McHelper.getMaxContentYExclusive(toWorld)
        );
        
        GlobalPortalStorage storage = GlobalPortalStorage.get(fromWorld);
        
        storage.addPortal(connectingPortal);
    }
    
    public static void connectMutually(
        ResourceKey<Level> up,
        ResourceKey<Level> down,
        boolean respectSpaceRatio
    ) {
        
        connect(up, ConnectorType.floor, down, respectSpaceRatio);
        connect(down, ConnectorType.ceil, up, respectSpaceRatio);
    }
    
    public static VerticalConnectingPortal createConnectingPortal(
        ServerLevel fromWorld,
        ConnectorType connectorType,
        ServerLevel toWorld,
        double scaling,
        boolean inverted,
        double rotationAlongYDegrees,
        int fromWorldMinY, int fromWorldMaxY, int toWorldMinY, int toWorldMaxY
    ) {
        VerticalConnectingPortal verticalConnectingPortal = new VerticalConnectingPortal(
            entityType, fromWorld
        );
        
        verticalConnectingPortal.dimensionTo = toWorld.dimension();
        verticalConnectingPortal.width = 23333333333.0d;
        verticalConnectingPortal.height = 23333333333.0d;
        
        switch (connectorType) {
            case floor:
                verticalConnectingPortal.setPos(0, fromWorldMinY, 0);
                verticalConnectingPortal.axisW = new Vec3(0, 0, 1);
                verticalConnectingPortal.axisH = new Vec3(1, 0, 0);
                break;
            case ceil:
                verticalConnectingPortal.setPos(0, fromWorldMaxY, 0);
                verticalConnectingPortal.axisW = new Vec3(1, 0, 0);
                verticalConnectingPortal.axisH = new Vec3(0, 0, 1);
                break;
        }
        
        
        if (!inverted) {
            switch (connectorType) {
                case floor:
                    verticalConnectingPortal.setDestination(new Vec3(0, toWorldMaxY, 0));
                    break;
                case ceil:
                    verticalConnectingPortal.setDestination(new Vec3(0, toWorldMinY, 0));
                    break;
            }
        }
        else {
            switch (connectorType) {
                case floor:
                    verticalConnectingPortal.setDestination(new Vec3(0, toWorldMinY, 0));
                    break;
                case ceil:
                    verticalConnectingPortal.setDestination(new Vec3(0, toWorldMaxY, 0));
                    break;
            }
        }
        
        DQuaternion inversionRotation = inverted ?
            DQuaternion.rotationByDegrees(new Vec3(1, 0, 0), 180) : null;
        DQuaternion additionalRotation = rotationAlongYDegrees != 0 ?
            DQuaternion.rotationByDegrees(new Vec3(0, 1, 0), rotationAlongYDegrees) : null;
        DQuaternion rotation = Helper.combineNullable(
            inversionRotation, additionalRotation, DQuaternion::hamiltonProduct
        );
        verticalConnectingPortal.rotation = rotation;
        
        if (scaling != 1.0) {
            verticalConnectingPortal.scaling = scaling;
            verticalConnectingPortal.teleportChangesScale = false;
            PortalExtension.get(verticalConnectingPortal).adjustPositionAfterTeleport = false;
        }
        
        return verticalConnectingPortal;
    }
    
    public static void removeConnectingPortal(
        ConnectorType connectorType,
        ResourceKey<Level> dimension
    ) {
        removeConnectingPortal(getPredicate(connectorType), dimension);
    }
    
    private static void removeConnectingPortal(
        Predicate<Portal> predicate, ResourceKey<Level> dimension
    ) {
        ServerLevel endWorld = MiscHelper.getServer().getLevel(dimension);
        GlobalPortalStorage storage = GlobalPortalStorage.get(endWorld);
        
        storage.removePortals(
            portal -> portal instanceof VerticalConnectingPortal && predicate.test(portal)
        );
    }
    
    public static VerticalConnectingPortal getConnectingPortal(
        Level world, ConnectorType type
    ) {
        return (VerticalConnectingPortal) GlobalPortalStorage.getGlobalPortals(world).stream()
            .filter(getPredicate(type))
            .findFirst().orElse(null);
    }
    
}
