package com.qouteall.immersive_portals.teleportation;

import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.my_util.Helper;
import com.qouteall.immersive_portals.portal_entity.Portal;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.dimension.DimensionType;

public class ServerTeleportationManager {
    
    public void onPlayerTeleportedInClient(
        ServerPlayerEntity player,
        int portalId
    ) {
        Entity entityPortal = player.world.getEntityById(portalId);
        assert player.dimension == player.world.dimension.getType();
        if (!(entityPortal instanceof Portal)) {
            Helper.err("Can Not Find Portal " + portalId + " in " + player.dimension + "to teleport");
            return;
        }
        
        teleportPlayer(player, ((Portal) entityPortal));
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
            ModMain.serverTaskList.addTask(() -> {
                changePlayerDimension(player, fromWorld, toWorld, newPos);
                return true;
            });
        }
        
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
    
}
