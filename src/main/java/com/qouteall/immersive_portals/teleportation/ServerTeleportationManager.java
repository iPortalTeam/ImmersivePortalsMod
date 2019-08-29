package com.qouteall.immersive_portals.teleportation;

import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.MyNetwork;
import com.qouteall.immersive_portals.exposer.IEServerPlayNetworkHandler;
import com.qouteall.immersive_portals.exposer.IEServerPlayerEntity;
import com.qouteall.immersive_portals.my_util.Helper;
import com.qouteall.immersive_portals.portal.Portal;
import net.minecraft.client.network.packet.CustomPayloadS2CPacket;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.dimension.DimensionType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.stream.Stream;

public class ServerTeleportationManager {
    private Set<ServerPlayerEntity> teleportingEntities = new HashSet<>();
    private WeakHashMap<ServerPlayerEntity, Long> lastTeleportGameTime = new WeakHashMap<>();
    
    public ServerTeleportationManager() {
        ModMain.postServerTickSignal.connectWithWeakRef(this, ServerTeleportationManager::tick);
        Portal.serverPortalTickSignal.connectWithWeakRef(
            this, (this_, portal) ->
                portal.getEntitiesToTeleport().forEach(entity -> {
                    if (!(entity instanceof ServerPlayerEntity)) {
                        ModMain.serverTaskList.addTask(() -> {
                            teleportRegularEntity(entity, portal);
                            return true;
                        });
                    }
                })
        );
    }
    
    public void onPlayerTeleportedInClient(
        ServerPlayerEntity player,
        DimensionType dimensionBefore,
        Vec3d posBefore,
        int portalId
    ) {
        Entity portalEntity = Helper.getServer()
            .getWorld(dimensionBefore).getEntityById(portalId);
        lastTeleportGameTime.put(player, Helper.getServerGameTime());
        
        if (canPlayerTeleport(player, dimensionBefore, posBefore, portalEntity)) {
            if (isTeleporting(player)) {
                Helper.err(player.toString() + "is teleporting frequently");
            }
    
            DimensionType dimensionTo = ((Portal) portalEntity).dimensionTo;
            Vec3d newPos = ((Portal) portalEntity).applyTransformationToPoint(posBefore);
    
            teleportPlayer(player, dimensionTo, newPos);
        }
        else {
            Helper.err(String.format(
                "Player cannot teleport through portal %s %s %s %s",
                player.getName().asString(),
                player.dimension,
                player.getPos(),
                portalId
            ));
        }
    }
    
    private boolean canPlayerTeleport(
        ServerPlayerEntity player,
        DimensionType dimensionBefore,
        Vec3d posBefore,
        Entity portalEntity
    ) {
        return canPlayerReachPos(player, dimensionBefore, posBefore) &&
            portalEntity instanceof Portal &&
            isClose(posBefore, portalEntity.getPos()) &&
            !player.hasVehicle();
    }
    
    private boolean canPlayerReachPos(
        ServerPlayerEntity player,
        DimensionType dimension,
        Vec3d pos
    ) {
        return player.dimension == dimension ?
            isClose(pos, player.getPos())
            :
            Helper.getEntitiesNearby(player, Portal.class, 10)
                .anyMatch(
                    portal -> portal.dimensionTo == dimension &&
                        isClose(pos, portal.destination)
                );
    }
    
    private static boolean isClose(Vec3d a, Vec3d b) {
        return a.squaredDistanceTo(b) < 15 * 15;
    }
    
    private void teleportPlayer(
        ServerPlayerEntity player,
        DimensionType dimensionTo,
        Vec3d newPos
    ) {
        ServerWorld fromWorld = (ServerWorld) player.world;
        ServerWorld toWorld = Helper.getServer().getWorld(dimensionTo);
    
        if (player.dimension == dimensionTo) {
            player.setPosition(newPos.x, newPos.y, newPos.z);
        }
        else {
            changePlayerDimension(player, fromWorld, toWorld, newPos);
            ((IEServerPlayNetworkHandler) player.networkHandler).cancelTeleportRequest();
        }
    
        ((IEServerPlayerEntity) player).setIsInTeleportationState(true);
        player.networkHandler.syncWithPlayerPosition();
    }
    
    /**
     * {@link ServerPlayerEntity#changeDimension(DimensionType)}
     */
    private void changePlayerDimension(
        ServerPlayerEntity player,
        ServerWorld fromWorld,
        ServerWorld toWorld,
        Vec3d destination
    ) {
        BlockPos oldPos = player.getBlockPos();
        
        teleportingEntities.add(player);
    
        //TODO fix travel when riding entity
        player.detach();
    
        fromWorld.removePlayer(player);
        player.removed = false;
    
        player.x = destination.x;
        player.y = destination.y;
        player.z = destination.z;
        
        player.world = toWorld;
        player.dimension = toWorld.dimension.getType();
        toWorld.respawnPlayer(player);
        
        toWorld.checkChunk(player);
        
        Helper.getServer().getPlayerManager().sendWorldInfo(
            player, toWorld
        );
        
        player.interactionManager.setWorld(toWorld);
    
        Helper.log(String.format(
            "%s teleported from %s %s to %s %s",
            player.getName(),
            fromWorld.dimension.getType(),
            oldPos,
            toWorld.dimension.getType(),
            player.getBlockPos()
        ));
    
        //this is used for the advancement of "we need to go deeper"
        //and the advancement of travelling for long distance through nether
        if (toWorld.dimension.getType() == DimensionType.THE_NETHER) {
            //this is used for
            ((IEServerPlayerEntity) player).setEnteredNetherPos(player.getPos());
        }
        ((IEServerPlayerEntity) player).updateDimensionTravelAdvancements(fromWorld);
    }
    
    private void sendPositionConfirmMessage(ServerPlayerEntity player) {
        CustomPayloadS2CPacket packet = MyNetwork.createStcDimensionConfirm(
            player.dimension,
            player.getPos()
        );
        
        player.networkHandler.sendPacket(packet);
    }
    
    private void tick() {
        teleportingEntities = new HashSet<>();
        long tickTimeNow = Helper.getServerGameTime();
        if (tickTimeNow % 10 == 7) {
            ArrayList<ServerPlayerEntity> copiedPlayerList =
                Helper.getCopiedPlayerList();
            for (ServerPlayerEntity player : copiedPlayerList) {
                Long lastTeleportGameTime =
                    this.lastTeleportGameTime.getOrDefault(player, 0L);
                if (tickTimeNow - lastTeleportGameTime > 60) {
                    sendPositionConfirmMessage(player);
                    ((IEServerPlayerEntity) player).setIsInTeleportationState(false);
                }
                else {
                    ((IEServerPlayNetworkHandler) player.networkHandler).cancelTeleportRequest();
                }
            }
        }
    }
    
    public boolean isTeleporting(ServerPlayerEntity entity) {
        return teleportingEntities.contains(entity);
    }
    
    private void teleportRegularEntity(Entity entity, Portal portal) {
        assert entity.dimension == portal.dimension;
        assert !(entity instanceof ServerPlayerEntity);
    
        if (entity.hasVehicle() || !entity.getPassengerList().isEmpty()) {
            return;
        }
        
        Vec3d newPos = portal.applyTransformationToPoint(entity.getPos());
        
        if (portal.dimensionTo != entity.dimension) {
            changeEntityDimension(entity, portal.dimensionTo, newPos);
        }
        
        entity.setPosition(
            newPos.x, newPos.y, newPos.z
        );
    }
    
    /**
     * {@link Entity#changeDimension(DimensionType)}
     */
    private void changeEntityDimension(
        Entity entity,
        DimensionType toDimension,
        Vec3d destination
    ) {
        ServerWorld fromWorld = (ServerWorld) entity.world;
        ServerWorld toWorld = Helper.getServer().getWorld(toDimension);
        entity.detach();
        
        Stream<ServerPlayerEntity> watchingPlayers = Helper.getEntitiesNearby(
            entity,
            ServerPlayerEntity.class,
            128
        );
        
        fromWorld.removeEntity(entity);
        entity.removed = false;
        
        entity.x = destination.x;
        entity.y = destination.y;
        entity.z = destination.z;
        
        entity.world = toWorld;
        entity.dimension = toDimension;
        toWorld.method_18769(entity);
    }
}
