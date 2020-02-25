package com.qouteall.immersive_portals.teleportation;

import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.ducks.IEClientPlayNetworkHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.dimension.DimensionType;

import java.util.Objects;
import java.util.Optional;

public class ClientPosSync {
    /**
     * {@link ClientPlayNetworkHandler#onPlayerPositionLook(PlayerPositionLookS2CPacket)}
     */
    
    private static final MinecraftClient client = MinecraftClient.getInstance();
    
    public static void init() {
        ModMain.postClientTickSignal.connect(ClientPosSync::tick);
    }
    
    public static void acceptForceSync(
        DimensionType dimension, Vec3d pos,
        Optional<Integer> vehicleId
    ) {
        ClientPlayerEntity player = client.player;
        
        if (player.removed) {
            Helper.err("Ignore position sync because client player is removed");
            return;
        }
        
        McHelper.checkDimension(player);
        
        updateVehicle(player, vehicleId);
        
        if (player.dimension != dimension) {
            CGlobal.clientTeleportationManager.changePlayerDimension(
                player,
                CGlobal.clientWorldLoader.getOrCreateFakedWorld(player.dimension),
                CGlobal.clientWorldLoader.getOrCreateFakedWorld(dimension),
                pos
            );
        }
        
        player.updatePosition(pos.x, pos.y, pos.z);
        McHelper.setPosAndLastTickPos(player, pos, pos);
        
        updateVehicle(player, vehicleId);
        
        CGlobal.clientTeleportationManager.disableTeleportFor2Seconds();
        
        ((IEClientPlayNetworkHandler) client.player.networkHandler).initScreenIfNecessary();
    }
    
    private static void updateVehicle(
        ClientPlayerEntity player, Optional<Integer> vehicleId
    ) {
        if (!vehicleId.isPresent()) {
            if (player.hasVehicle()) {
                player.stopRiding();
            }
        }
        else {
            int vehicleIdInt = vehicleId.get();
            Entity vehicle = retrieveClientEntityAcrossWorlds(vehicleIdInt);
            if (vehicle == null) {
                Helper.err("Ouch! Vehicle does not exist in client");
                return;
            }
            
            if (player.hasVehicle()) {
                if (player.getVehicle() != vehicle) {
                    Helper.log("Force changed client player vehicle");
                    forceAdjustVehicleClient(vehicle, player);
                    player.startRiding(vehicle, true);
                }
            }
            else {
                Helper.log("Force client player to start riding");
                forceAdjustVehicleClient(vehicle, player);
                player.startRiding(vehicle, true);
            }
        }
    }
    
    private static Entity retrieveClientEntityAcrossWorlds(
        int entityId
    ) {
        return CGlobal.clientWorldLoader.clientWorldMap.values().stream().map(
            world -> world.getEntityById(entityId)
        ).filter(Objects::nonNull).findFirst().orElse(null);
    }
    
    public static void forceAdjustVehicleClient(
        Entity vehicle,
        PlayerEntity player
    ) {
        Vec3d vehiclePos = new Vec3d(
            player.getX(),
            McHelper.getVehicleY(vehicle, player),
            player.getZ()
        );
        ClientTeleportationManager.moveClientEntityAcrossDimension(
            vehicle, ((ClientWorld) player.world),
            vehiclePos
        );
    }
    
    private static void tick() {
        ClientPlayerEntity player = client.player;
        if (player.prevX != player.getX() ||
            player.prevY != player.getY() ||
            player.prevZ != player.getZ()
        ) {
            sendPositionSyncPacket();
        }
    }
    
    public static void sendPositionSyncPacket() {
    
    }
}
