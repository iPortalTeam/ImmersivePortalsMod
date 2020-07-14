package com.qouteall.immersive_portals.mixin.position_sync;

import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.ducks.IEPlayerMoveC2SPacket;
import com.qouteall.immersive_portals.ducks.IEPlayerPositionLookS2CPacket;
import com.qouteall.immersive_portals.ducks.IEServerPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class MixinServerPlayNetworkHandler implements IEServerPlayNetworkHandler {
    @Shadow
    public ServerPlayerEntity player;
    @Shadow
    private Vec3d requestedTeleportPos;
    @Shadow
    private int requestedTeleportId;
    @Shadow
    private int teleportRequestTick;
    @Shadow
    private int ticks;
    
    @Shadow
    private boolean ridingEntity;
    
    @Shadow
    private double updatedRiddenX;
    
    @Shadow
    private double updatedRiddenY;
    
    @Shadow
    private double updatedRiddenZ;
    
    @Shadow protected abstract boolean isHost();
    
    @Shadow protected abstract boolean isPlayerNotCollidingWithBlocks(WorldView worldView, Box box);
    
    //do not process move packet when client dimension and server dimension are not synced
    @Inject(
        method = "onPlayerMove",
        at = @At(
            value = "INVOKE",
            shift = At.Shift.AFTER,
            target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/server/world/ServerWorld;)V"
        ),
        cancellable = true
    )
    private void onProcessMovePacket(PlayerMoveC2SPacket packet, CallbackInfo ci) {
        RegistryKey<World> packetDimension = ((IEPlayerMoveC2SPacket) packet).getPlayerDimension();
        
        assert packetDimension != null;
        
        if (Global.serverTeleportationManager.isJustTeleported(player, 100)) {
            cancelTeleportRequest();
        }
        
        if (player.world.getRegistryKey() != packetDimension) {
            Global.serverTeleportationManager.acceptDubiousMovePacket(
                player, packet, packetDimension
            );
            ci.cancel();
        }
    }
    
    @Redirect(
        method = "onPlayerMove",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerPlayNetworkHandler;isHost()Z"
        )
    )
    private boolean redirectIsServerOwnerOnPlayerMove(ServerPlayNetworkHandler serverPlayNetworkHandler) {
        if (Global.looseMovementCheck) {
            Helper.log("Accepted dubious movement " + player.getName().getString());
            return true;
        }
        return isHost();
    }
    
    /**
     * make PlayerPositionLookS2CPacket contain dimension data
     *
     * @author qouteall
     */
    @Overwrite
    public void teleportRequest(
        double destX,
        double destY,
        double destZ,
        float destYaw,
        float destPitch,
        Set<PlayerPositionLookS2CPacket.Flag> updates
    ) {
        Helper.log(String.format("request teleport %s %s (%d %d %d)->(%d %d %d)",
            player.getName().asString(),
            player.world.getRegistryKey(),
            (int) player.getX(), (int) player.getY(), (int) player.getZ(),
            (int) destX, (int) destY, (int) destZ
        ));
        
        double currX = updates.contains(PlayerPositionLookS2CPacket.Flag.X) ? this.player.getX() : 0.0D;
        double currY = updates.contains(PlayerPositionLookS2CPacket.Flag.Y) ? this.player.getY() : 0.0D;
        double currZ = updates.contains(PlayerPositionLookS2CPacket.Flag.Z) ? this.player.getZ() : 0.0D;
        float currYaw = updates.contains(PlayerPositionLookS2CPacket.Flag.Y_ROT) ? this.player.yaw : 0.0F;
        float currPitch = updates.contains(PlayerPositionLookS2CPacket.Flag.X_ROT) ? this.player.pitch : 0.0F;
        
        if (!Global.serverTeleportationManager.isJustTeleported(player, 100)) {
            this.requestedTeleportPos = new Vec3d(destX, destY, destZ);
        }
        
        if (++this.requestedTeleportId == Integer.MAX_VALUE) {
            this.requestedTeleportId = 0;
        }
        
        this.teleportRequestTick = this.ticks;
        this.player.updatePositionAndAngles(destX, destY, destZ, destYaw, destPitch);
        PlayerPositionLookS2CPacket lookPacket = new PlayerPositionLookS2CPacket(
            destX - currX,
            destY - currY,
            destZ - currZ,
            destYaw - currYaw,
            destPitch - currPitch,
            updates,
            this.requestedTeleportId
        );
        //noinspection ConstantConditions
        ((IEPlayerPositionLookS2CPacket) lookPacket).setPlayerDimension(player.world.getRegistryKey());
        this.player.networkHandler.sendPacket(lookPacket);
        
        if (Global.teleportationDebugEnabled) {
            new Throwable().printStackTrace();
        }
    }
    
    //server will check the collision when receiving position packet from client
    //we treat collision specially when player is halfway through a portal
    //"isPlayerNotCollidingWithBlocks" is wrong now
    @Redirect(
        method = "onPlayerMove",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerPlayNetworkHandler;isPlayerNotCollidingWithBlocks(Lnet/minecraft/world/WorldView;Lnet/minecraft/util/math/Box;)Z"
        )
    )
    private boolean onCheckPlayerCollision(
        ServerPlayNetworkHandler serverPlayNetworkHandler,
        WorldView worldView,
        Box box
    ) {
        if (Global.serverTeleportationManager.isJustTeleported(player, 100)) {
            return true;
        }
        boolean portalsNearby = McHelper.getServerPortalsNearby(
            player,
            5
        ).findAny().isPresent();
        if (portalsNearby) {
            return true;
        }
        return isPlayerNotCollidingWithBlocks(worldView, box);
    }
    
    @Inject(
        method = "onTeleportConfirm",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onOnTeleportConfirm(TeleportConfirmC2SPacket packet, CallbackInfo ci) {
        if (requestedTeleportPos == null) {
            ci.cancel();
        }
    }
    
    //do not reject move when player is riding and entering portal
    //the client packet is not validated (validating it needs dimension info in packet)
    @Inject(
        method = "onVehicleMove",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerPlayNetworkHandler;validateVehicleMove(Lnet/minecraft/network/packet/c2s/play/VehicleMoveC2SPacket;)Z"
        ),
        cancellable = true
    )
    private void onOnVehicleMove(VehicleMoveC2SPacket packet, CallbackInfo ci) {
        if (Global.serverTeleportationManager.isJustTeleported(player, 40)) {
            Entity entity = this.player.getRootVehicle();
            
            if (entity != player) {
                double currX = entity.getX();
                double currY = entity.getY();
                double currZ = entity.getZ();
                
                double newX = packet.getX();
                double newY = packet.getY();
                double newZ = packet.getZ();
                
                if (entity.getPos().squaredDistanceTo(
                    newX, newY, newZ
                ) < 256) {
                    float yaw = packet.getYaw();
                    float pitch = packet.getPitch();
                    
                    entity.updatePositionAndAngles(newX, newY, newZ, yaw, pitch);
                    
                    this.player.getServerWorld().getChunkManager().updateCameraPosition(this.player);
                    
                    ridingEntity = true;
                    updatedRiddenX = entity.getX();
                    updatedRiddenY = entity.getY();
                    updatedRiddenZ = entity.getZ();
                }
            }
            
            ci.cancel();
        }
    }
    
    @Override
    public void cancelTeleportRequest() {
        requestedTeleportPos = null;
    }
}
