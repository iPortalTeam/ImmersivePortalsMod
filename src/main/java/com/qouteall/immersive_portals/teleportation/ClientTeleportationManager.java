package com.qouteall.immersive_portals.teleportation;

import com.qouteall.immersive_portals.Globals;
import com.qouteall.immersive_portals.exposer.IEClientPlayNetworkHandler;
import com.qouteall.immersive_portals.exposer.IEClientWorld;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.packet.PlayerRespawnS2CPacket;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.dimension.DimensionType;

public class ClientTeleportationManager {
    MinecraftClient mc = MinecraftClient.getInstance();
    
    public ClientTeleportationManager() {
    }
    
    /**
     * {@link ClientPlayNetworkHandler#onPlayerRespawn(PlayerRespawnS2CPacket)}
     */
    public void finishTeleportingPlayer(DimensionType toDimension, Vec3d destination) {
        ClientWorld fromWorld = mc.world;
        DimensionType fromDimension = fromWorld.dimension.getType();
        ClientPlayerEntity player = mc.player;
        if (fromDimension == toDimension) {
            player.setPosition(
                destination.x,
                destination.y,
                destination.z
            );
            return;
        }
        
        ClientWorld toWorld = Globals.clientWorldLoader.getOrCreateFakedWorld(toDimension);
        
        ClientPlayNetworkHandler workingNetHandler = ((IEClientWorld) fromWorld).getNetHandler();
        ClientPlayNetworkHandler fakedNetHandler = ((IEClientWorld) toWorld).getNetHandler();
        ((IEClientPlayNetworkHandler) workingNetHandler).setWorld(toWorld);
        ((IEClientPlayNetworkHandler) fakedNetHandler).setWorld(fromWorld);
        
        fromWorld.removeEntity(player.getEntityId());
        player.removed = false;
        player.world = toWorld;
        player.dimension = toWorld.dimension.getType();
        player.x = destination.x;
        player.y = destination.y;
        player.z = destination.z;
        
        toWorld.addPlayer(player.getEntityId(), player);
        
        mc.world = toWorld;
        mc.worldRenderer = Globals.clientWorldLoader.getWorldRenderer(toDimension);
        
        toWorld.setScoreboard(fromWorld.getScoreboard());
        
        if (mc.particleManager != null)
            mc.particleManager.setWorld(toWorld);
    
        BlockEntityRenderDispatcher.INSTANCE.setWorld(toWorld);
    }
}
