package com.qouteall.immersive_portals.teleportation;

import com.google.common.primitives.Doubles;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.SGlobal;
import net.minecraft.entity.MovementType;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.dimension.DimensionType;

public class ServerPosSync {
    /**
     * {@link ServerPlayNetworkHandler#onPlayerMove(PlayerMoveC2SPacket)}
     * {@link ServerPlayNetworkHandler#onVehicleMove(VehicleMoveC2SPacket)}
     * consider: bed, elytra, knockback, jump
     */
    
    //TODO consider hacked client and check fly, cross wall, packet spam
    public static void acceptPositionPacketFromClient(
        ServerPlayerEntity player,
        DimensionType newDimension,
        Vec3d newPos,
        float yaw, float pitch,
        boolean newIsOnGround
    ) {
        McHelper.checkDimension(player);
        
        if (!isFinite(newPos)) {
            Helper.log("Infinite pos?");
            return;
        }
        
        if (player.notInAnyWorld) {
            Helper.log("Ignore pos packet because player is not in any world " + player.getName().asString());
            return;
        }
        
        boolean isNormalMovement =
            handleNormalMovement(player, newDimension, newPos, newIsOnGround);
        
        if (!isNormalMovement) {
            if (canPlayerReachPos(player, newDimension, newPos)) {
                SGlobal.serverTeleportationManager.teleportPlayer(
                    player, newDimension, newPos
                );
            }
            else {
                Helper.log(String.format(
                    "Illegal Player Movement %s (%s %s %s %s)->(%s %s %s %s)",
                    player.getName().asString(),
                    player.dimension, (int) player.getX(), (int) player.getY(), (int) player.getZ(),
                    newDimension, (int) newPos.getX(), (int) newPos.getY(), (int) newPos.getZ()
                ));
                sendForcePositionSync(player);
            }
        }
        
        player.getServerWorld().getChunkManager().updateCameraPosition(player);
    }
    
    private static boolean handleNormalMovement(
        ServerPlayerEntity player,
        DimensionType newDimension,
        Vec3d newPos,
        boolean newIsOnGround
    ) {
        Vec3d oldPos = player.getPos();
        if (player.dimension != newDimension ||
            oldPos.squaredDistanceTo(newPos) > 256
        ) {
            return false;
        }
        
        if (player.isInTeleportationState()) {
            return false;
        }
        
        Vec3d delta = newPos.subtract(oldPos);
        if (player.onGround && !newIsOnGround && delta.getY() > 0) {
            player.jump();
        }
        if (delta.getY() > 0) {
            player.fallDistance = 0;
        }
        
        player.move(MovementType.PLAYER, delta);
        player.onGround = newIsOnGround;
        
        Vec3d movedPos = player.getPos();
        
        if (movedPos.squaredDistanceTo(newPos) > 1) {
            player.updatePosition(oldPos.x, oldPos.y, oldPos.z);
            return false;
        }
        
        player.handleFall(movedPos.y - oldPos.y, newIsOnGround);
        
        return true;
    }

//    private static boolean isMovementValid(
//        ServerPlayerEntity player,
//        DimensionType dimension,
//        Vec3d pos
//    ) {
//        return canPlayerReachPos(player, dimension, pos) &&
//            player.world.doesNotCollide(
//                player, player.getBoundingBox().contract(9.999999747378752E-6D)
//            );
//    }
    
    
    private static boolean canPlayerReachPos(
        ServerPlayerEntity player,
        DimensionType dimension,
        Vec3d pos
    ) {
        if (player.isInTeleportationState()) {
            return true;
        }
        
        Vec3d playerPos = player.getPos();
        if (player.dimension == dimension) {
            if (playerPos.squaredDistanceTo(pos) < 256) {
                return true;
            }
        }
        return McHelper.getServerPortalsNearby(player, 16)
            .filter(portal -> portal.dimensionTo == dimension)
            .map(portal -> portal.applyTransformationToPoint(playerPos))
            .anyMatch(mappedPos -> mappedPos.squaredDistanceTo(pos) < 256);
    }
    
    public static void sendForcePositionSync(ServerPlayerEntity player) {
    
    }
    
    private static boolean isFinite(Vec3d pos) {
        return Doubles.isFinite(pos.x) &&
            Doubles.isFinite(pos.y) &&
            Doubles.isFinite(pos.z);
    }
}
