package qouteall.imm_ptl.core.api;

import qouteall.imm_ptl.core.Helper;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.chunk_loading.ChunkLoader;
import qouteall.imm_ptl.core.chunk_loading.NewChunkTrackingGraph;
import qouteall.q_misc_util.my_util.DQuaternion;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalManipulation;
import qouteall.imm_ptl.core.portal.global_portals.GlobalPortalStorage;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Pair;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class PortalAPI {
    
    public static void setPortalPositionOrientationAndSize(
        Portal portal,
        Vec3d position,
        DQuaternion orientation,
        double width, double height
    ) {
        portal.setOriginPos(position);
        portal.setOrientationAndSize(
            orientation.rotate(new Vec3d(1, 0, 0)),
            orientation.rotate(new Vec3d(0, 1, 0)),
            width, height
        );
    }
    
    public static void setPortalOrthodoxShape(Portal portal, Direction facing, Box portalArea) {
        Pair<Direction, Direction> directions = Helper.getPerpendicularDirections(facing);
        
        Vec3d areaSize = Helper.getBoxSize(portalArea);
        
        Box boxSurface = Helper.getBoxSurface(portalArea, facing);
        Vec3d center = boxSurface.getCenter();
        portal.setPosition(center.x, center.y, center.z);
        
        portal.axisW = Vec3d.of(directions.getLeft().getVector());
        portal.axisH = Vec3d.of(directions.getRight().getVector());
        portal.width = Helper.getCoordinate(areaSize, directions.getLeft().getAxis());
        portal.height = Helper.getCoordinate(areaSize, directions.getRight().getAxis());
    }
    
    public static void setPortalTransformation(
        Portal portal,
        RegistryKey<World> destinationDimension,
        Vec3d destinationPosition,
        @Nullable DQuaternion rotation,
        double scale
    ) {
        portal.setDestinationDimension(destinationDimension);
        portal.setDestination(destinationPosition);
        portal.setRotationTransformation(rotation.toMcQuaternion());
        portal.setScaleTransformation(scale);
    }
    
    public static DQuaternion getPortalOrientationQuaternion(Portal portal) {
        return PortalManipulation.getPortalOrientationQuaternion(portal.axisW, portal.axisH);
    }
    
    public static void setPortalOrientationQuaternion(Portal portal, DQuaternion quaternion) {
        PortalManipulation.setPortalOrientationQuaternion(portal, quaternion);
    }
    
    public static void spawnServerEntity(Entity entity) {
        McHelper.spawnServerEntity(entity);
    }
    
    public static <T extends Portal> T createReversePortal(T portal) {
        return (T) PortalManipulation.createReversePortal(
            portal, (EntityType<? extends Portal>) portal.getType()
        );
    }
    
    public static <T extends Portal> T createFlippedPortal(T portal) {
        return (T) PortalManipulation.createFlippedPortal(
            portal, (EntityType<? extends Portal>) portal.getType()
        );
    }
    
    public static <T extends Portal> T copyPortal(Portal portal, EntityType<T> entityType) {
        return (T) PortalManipulation.copyPortal(portal, (EntityType<Portal>) entityType);
    }
    
    public static void addGlobalPortal(
        ServerWorld world, Portal portal
    ) {
        McHelper.validateOnServerThread();
        GlobalPortalStorage.get(world).addPortal(portal);
    }
    
    public static void removeGlobalPortal(
        ServerWorld world, Portal portal
    ) {
        McHelper.validateOnServerThread();
        GlobalPortalStorage.get(world).removePortal(portal);
    }
    
    public static void addChunkLoaderForPlayer(ServerPlayerEntity player, ChunkLoader chunkLoader) {
        McHelper.validateOnServerThread();
        NewChunkTrackingGraph.addPerPlayerAdditionalChunkLoader(player, chunkLoader);
    }
    
    public static void removeChunkLoaderForPlayer(ServerPlayerEntity player, ChunkLoader chunkLoader) {
        McHelper.validateOnServerThread();
        NewChunkTrackingGraph.removePerPlayerAdditionalChunkLoader(player, chunkLoader);
    }
    
}
