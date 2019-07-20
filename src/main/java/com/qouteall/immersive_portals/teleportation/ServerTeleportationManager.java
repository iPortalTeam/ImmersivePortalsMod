package com.qouteall.immersive_portals.teleportation;

import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.MyNetwork;
import com.qouteall.immersive_portals.my_util.Helper;
import com.qouteall.immersive_portals.portal_entity.Portal;
import net.minecraft.client.network.packet.CustomPayloadS2CPacket;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.dimension.DimensionType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class ServerTeleportationManager {
    private Set<ServerPlayerEntity> teleportingEntities = new HashSet<>();
    
    public ServerTeleportationManager() {
        ModMain.postServerTickSignal.connectWithWeakRef(this, ServerTeleportationManager::tick);
    }
    
    public void onPlayerTeleportedInClient(
        ServerPlayerEntity player,
        DimensionType dimensionBefore,
        Vec3d posBefore,
        int portalId
    ) {
        Entity portalEntity = Helper.getServer()
            .getWorld(dimensionBefore).getEntityById(portalId);
    
        if (canPlayerTeleport(player, dimensionBefore, posBefore, portalEntity)) {
            if (isTeleporting(player)) {
                Helper.log(player.toString() + "tried to teleport for multiple times. rejected.");
                return;
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
            sendPositionConfirmMessage(player);
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
            isClose(posBefore, portalEntity.getPos());
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
        return a.squaredDistanceTo(b) < 10 * 10;
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
        }
    
        sendConfirmMessageTwice(player);
    }
    
    /**
     * {@link ServerPlayerEntity#changeDimension(DimensionType)}
     */
    private void changePlayerDimension(
        ServerPlayerEntity player,
        ServerWorld fromWorld,
        ServerWorld toWorld,
        Vec3d newPos
    ) {
        teleportingEntities.add(player);
        
        fromWorld.removeEntity(player);
        player.removed = false;
        
        player.x = newPos.x;
        player.y = newPos.y;
        player.z = newPos.z;
        
        player.world = toWorld;
        player.dimension = toWorld.dimension.getType();
        toWorld.respawnPlayer(player);
        
        toWorld.checkChunk(player);
        
        Helper.getServer().getPlayerManager().sendWorldInfo(
            player, toWorld
        );
        
        player.interactionManager.setWorld(toWorld);
    
        Helper.log(String.format(
            "%s changed dimension on server from %s to %s",
            player,
            fromWorld.dimension.getType(),
            toWorld.dimension.getType()
        ));
    }
    
    private void sendConfirmMessageTwice(ServerPlayerEntity player) {
        //send a confirm message now
        //and send one again after 1 second
        
        sendPositionConfirmMessage(player);
        
        long startTickTime = Helper.getServerGameTime();
        ModMain.serverTaskList.addTask(() -> {
            if (Helper.getServerGameTime() - startTickTime > 20) {
                sendPositionConfirmMessage(player);
                return true;
            }
            else {
                return false;
            }
        });
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
        if (Helper.getServerGameTime() % 100 == 42) {
            ArrayList<ServerPlayerEntity> copyPlayerList =
                new ArrayList<>(Helper.getServer().getPlayerManager().getPlayerList());
            copyPlayerList.forEach(this::sendPositionConfirmMessage);
        }
    }
    
    public boolean isTeleporting(ServerPlayerEntity entity) {
        return teleportingEntities.contains(entity);
    }
    
}
