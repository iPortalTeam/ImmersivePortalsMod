package qouteall.imm_ptl.core.api;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.chunk_loading.ChunkLoader;
import qouteall.imm_ptl.core.chunk_loading.ImmPtlChunkTracking;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalManipulation;
import qouteall.imm_ptl.core.portal.global_portals.GlobalPortalStorage;
import qouteall.imm_ptl.core.teleportation.ServerTeleportationManager;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.dimension.DimensionIntId;
import qouteall.q_misc_util.my_util.DQuaternion;
import qouteall.q_misc_util.my_util.IntBox;

public class PortalAPI {
    
    public static void setPortalPositionOrientationAndSize(
        Portal portal,
        Vec3 position,
        DQuaternion orientation,
        double width, double height
    ) {
        portal.setOriginPos(position);
        portal.setOrientationAndSize(
            McHelper.getAxisWFromOrientation(orientation),
            McHelper.getAxisHFromOrientation(orientation),
            width, height
        );
    }
    
    public static void setPortalOrthodoxShape(Portal portal, Direction facing, AABB portalArea) {
        Tuple<Direction, Direction> directions = Helper.getPerpendicularDirections(facing);
        
        Vec3 areaSize = Helper.getBoxSize(portalArea);
        
        AABB boxSurface = Helper.getBoxSurface(portalArea, facing);
        Vec3 center = boxSurface.getCenter();
        portal.setPos(center.x, center.y, center.z);
        
        portal.axisW = Vec3.atLowerCornerOf(directions.getA().getNormal());
        portal.axisH = Vec3.atLowerCornerOf(directions.getB().getNormal());
        portal.width = Helper.getCoordinate(areaSize, directions.getA().getAxis());
        portal.height = Helper.getCoordinate(areaSize, directions.getB().getAxis());
    }
    
    public static void setPortalTransformation(
        Portal portal,
        ResourceKey<Level> destinationDimension,
        Vec3 destinationPosition,
        @Nullable DQuaternion rotation,
        double scale
    ) {
        portal.setDestinationDimension(destinationDimension);
        portal.setDestination(destinationPosition);
        portal.setRotation(rotation);
        portal.setScaleTransformation(scale);
    }
    
    public static DQuaternion getPortalOrientationQuaternion(Portal portal) {
        return PortalManipulation.getPortalOrientationQuaternion(portal.axisW, portal.axisH);
    }
    
    public static void setPortalOrientationQuaternion(Portal portal, DQuaternion quaternion) {
        PortalManipulation.setPortalOrientationQuaternion(portal, quaternion);
    }
    
    /**
     * Using entity.level().addFreshEntity(entity) is enough
     */
    @Deprecated
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
        ServerLevel world, Portal portal
    ) {
        McHelper.validateOnServerThread();
        GlobalPortalStorage.get(world).addPortal(portal);
    }
    
    public static void removeGlobalPortal(
        ServerLevel world, Portal portal
    ) {
        McHelper.validateOnServerThread();
        GlobalPortalStorage.get(world).removePortal(portal);
    }
    
    public static void addChunkLoaderForPlayer(ServerPlayer player, ChunkLoader chunkLoader) {
        McHelper.validateOnServerThread();
        ImmPtlChunkTracking.addPerPlayerAdditionalChunkLoader(player, chunkLoader);
    }
    
    public static void removeChunkLoaderForPlayer(ServerPlayer player, ChunkLoader chunkLoader) {
        McHelper.validateOnServerThread();
        ImmPtlChunkTracking.removePerPlayerAdditionalChunkLoader(player, chunkLoader);
    }
    
    /**
     * It can teleport the player without loading screen. Can also teleport regular entities.
     * @return the new entity (for player, it will be the same object)
     */
    public static Entity teleportEntity(Entity entity, ServerLevel targetWorld, Vec3 targetPos) {
        return ServerTeleportationManager.teleportEntityGeneral(entity, targetPos, targetWorld);
    }
    
    public static void syncBlockUpdateToClientImmediately(
        ServerLevel world, IntBox box
    ) {
        ImmPtlChunkTracking.syncBlockUpdateToClientImmediately(world, box);
    }
    
    @Environment(EnvType.CLIENT)
    public static int clientDimKeyToInt(ResourceKey<Level> dimension) {
        return DimensionIntId.getClientMap().toIntegerId(dimension);
    }
    
    @Environment(EnvType.CLIENT)
    public static ResourceKey<Level> clientIntToDimKey(int integerId) {
        return DimensionIntId.getClientMap().fromIntegerId(integerId);
    }
    
    public static int serverDimKeyToInt(MinecraftServer server, ResourceKey<Level> dimension) {
        return DimensionIntId.getServerMap(server).toIntegerId(dimension);
    }
    
    public static ResourceKey<Level> serverIntToDimKey(MinecraftServer server, int integerId) {
        return DimensionIntId.getServerMap(server).fromIntegerId(integerId);
    }
}
