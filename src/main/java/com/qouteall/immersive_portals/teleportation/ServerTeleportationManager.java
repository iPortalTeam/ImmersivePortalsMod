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

public class ServerTeleportationManager {
    
    public ServerTeleportationManager() {
        ModMain.postServerTickSignal.connectWithWeakRef(this, ServerTeleportationManager::tick);
    }
    
    public void onPlayerTeleportedInClient(
        ServerPlayerEntity player,
        int portalId
    ) {
        Entity portalEntity = player.world.getEntityById(portalId);
        assert player.dimension == player.world.dimension.getType();
        if (!(portalEntity instanceof Portal)) {
            Helper.err("Can Not Find Portal " + portalId + " in " + player.dimension + " to teleport");
            return;
        }
    
        if (canPlayerTeleportThrough(player, ((Portal) portalEntity))) {
            teleportPlayer(player, ((Portal) portalEntity));
        }
        else {
            Helper.err("Player cannot teleport through portal");
            sendPositionConfirmMessage(player);
        }
    }
    
    private boolean canPlayerTeleportThrough(
        ServerPlayerEntity player,
        Portal portal
    ) {
        return player.dimension == portal.dimension &&
            (player.getPos().squaredDistanceTo(portal.getPos()) < 10 * 10);
        
    }
    
    private void teleportPlayer(
        ServerPlayerEntity player,
        Portal portal
    ) {
        assert player.dimension == portal.dimension;
        
        ServerWorld fromWorld = (ServerWorld) player.world;
        ServerWorld toWorld = Helper.getServer().getWorld(portal.dimensionTo);
        Vec3d newPos = portal.applyTransformationToPoint(player.getPos());
    
        if (player.dimension == portal.dimensionTo) {
            player.setPosition(
                newPos.x,
                newPos.y,
                newPos.z
            );
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
        if (Helper.getServerGameTime() % 100 == 42) {
            ArrayList<ServerPlayerEntity> copyPlayerList =
                new ArrayList<>(Helper.getServer().getPlayerManager().getPlayerList());
            copyPlayerList.forEach(this::sendPositionConfirmMessage);
        }
    }
    
}
