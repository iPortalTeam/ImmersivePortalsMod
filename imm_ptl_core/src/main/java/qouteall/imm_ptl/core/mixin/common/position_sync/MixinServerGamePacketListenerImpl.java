package qouteall.imm_ptl.core.mixin.common.position_sync;

import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.IPMcHelper;
import qouteall.imm_ptl.core.ducks.IEEntity;
import qouteall.imm_ptl.core.ducks.IEPlayerMoveC2SPacket;
import qouteall.imm_ptl.core.ducks.IEPlayerPositionLookS2CPacket;
import qouteall.imm_ptl.core.ducks.IEServerPlayNetworkHandler;
import qouteall.imm_ptl.core.miscellaneous.IPVanillaCopy;
import qouteall.q_misc_util.Helper;

import java.util.Set;

@Mixin(value = ServerGamePacketListenerImpl.class, priority = 900)
public abstract class MixinServerGamePacketListenerImpl implements IEServerPlayNetworkHandler {
    @Shadow
    public ServerPlayer player;
    @Shadow
    private Vec3 awaitingPositionFromClient;
    @Shadow
    private int awaitingTeleport;
    @Shadow
    private int awaitingTeleportTime;
    @Shadow
    private int tickCount;
    
    @Shadow
    private double vehicleLastGoodX;
    
    @Shadow
    private double vehicleLastGoodY;
    
    @Shadow
    private double vehicleLastGoodZ;
    
    @Shadow
    protected abstract boolean isSingleplayerOwner();
    
    @Shadow
    protected abstract boolean isPlayerCollidingWithAnythingNew(LevelReader worldView, AABB box);
    
    @Shadow
    public abstract void disconnect(Component reason);
    
    @Shadow
    private double vehicleFirstGoodX;
    
    @Shadow
    private double vehicleFirstGoodY;
    
    @Shadow
    private double vehicleFirstGoodZ;
    
    @Shadow
    @Final
    public Connection connection;
    
    @Shadow
    @Final
    private MinecraftServer server;
    
    @Shadow
    private Entity lastVehicle;
    
    @Shadow
    private boolean clientVehicleIsFloating;
    
    //do not process move packet when client dimension and server dimension are not synced
    @Inject(
        method = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;handleMovePlayer(Lnet/minecraft/network/protocol/game/ServerboundMovePlayerPacket;)V",
        at = @At(
            value = "INVOKE",
            shift = At.Shift.AFTER,
            target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V"
        ),
        cancellable = true
    )
    private void onProcessMovePacket(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        ResourceKey<Level> packetDimension = ((IEPlayerMoveC2SPacket) packet).getPlayerDimension();
        
        if (packetDimension == null) {
            Helper.err("Player move packet is missing dimension info. Maybe the player client doesn't have ImmPtl");
            IPGlobal.serverTaskList.addTask(() -> {
                player.connection.disconnect(Component.literal(
                    "The client does not have Immersive Portals mod"
                ));
                return true;
            });
            return;
        }
        
        if (IPGlobal.serverTeleportationManager.isJustTeleported(player, 100)) {
            cancelTeleportRequest();
        }
        
        if (player.level.dimension() != packetDimension) {
            IPGlobal.serverTaskList.addTask(() -> {
                IPGlobal.serverTeleportationManager.acceptDubiousMovePacket(
                    player, packet, packetDimension
                );
                return true;
            });
            ci.cancel();
        }
    }
    
    @Redirect(
        method = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;handleMovePlayer(Lnet/minecraft/network/protocol/game/ServerboundMovePlayerPacket;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;isSingleplayerOwner()Z"
        )
    )
    private boolean redirectIsServerOwnerOnPlayerMove(ServerGamePacketListenerImpl serverPlayNetworkHandler) {
        if (shouldAcceptDubiousMovement(player)) {
            return true;
        }
        return isSingleplayerOwner();
    }
    
    @Redirect(
        method = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;handleMovePlayer(Lnet/minecraft/network/protocol/game/ServerboundMovePlayerPacket;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;isChangingDimension()Z"
        ),
        require = 0 // don't crash with carpet
    )
    private boolean redirectIsInTeleportationState(ServerPlayer player) {
        if (shouldAcceptDubiousMovement(player)) {
            return true;
        }
        return player.isChangingDimension();
    }
    
    /**
     * @reason make PlayerPositionLookS2CPacket contain dimension data and do some special handling
     * @author qouteall
     */
    @Overwrite
    @IPVanillaCopy
    public void teleport(double x, double y, double z, float yaw, float pitch, Set<ClientboundPlayerPositionPacket.RelativeArgument> flags, boolean shouldDismount) {
        // it may request teleport while this.player is marked removed during respawn
        
        if (player.getRemovalReason() != null) {
            Helper.err("Tries to send player pos packet to a removed player");
            new Throwable().printStackTrace();
            return;
        }
        
        double d = flags.contains(ClientboundPlayerPositionPacket.RelativeArgument.X) ? this.player.getX() : 0.0D;
        double e = flags.contains(ClientboundPlayerPositionPacket.RelativeArgument.Y) ? this.player.getY() : 0.0D;
        double f = flags.contains(ClientboundPlayerPositionPacket.RelativeArgument.Z) ? this.player.getZ() : 0.0D;
        float g = flags.contains(ClientboundPlayerPositionPacket.RelativeArgument.Y_ROT) ? this.player.getYRot() : 0.0F;
        float h = flags.contains(ClientboundPlayerPositionPacket.RelativeArgument.X_ROT) ? this.player.getXRot() : 0.0F;
        this.awaitingPositionFromClient = new Vec3(x, y, z);
        if (++this.awaitingTeleport == Integer.MAX_VALUE) {
            this.awaitingTeleport = 0;
        }

//        if (IPGlobal.serverTeleportationManager.isJustTeleported(player, 100)) {
//            Helper.err("Teleport request cancelled " + player.getName().asString());
//            return;
//        }
        
        this.awaitingTeleportTime = this.tickCount;
        this.player.absMoveTo(x, y, z, yaw, pitch);
        ClientboundPlayerPositionPacket lookPacket = new ClientboundPlayerPositionPacket(x - d, y - e, z - f, yaw - g, pitch - h, flags, this.awaitingTeleport, shouldDismount);
        
        ((IEPlayerPositionLookS2CPacket) lookPacket).setPlayerDimension(player.level.dimension());
        
        this.player.connection.send(lookPacket);
    }
    
    //server will check the collision when receiving position packet from client
    //we treat collision specially when player is halfway through a portal
    //"isPlayerNotCollidingWithBlocks" is wrong now
    @Redirect(
        method = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;handleMovePlayer(Lnet/minecraft/network/protocol/game/ServerboundMovePlayerPacket;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;isPlayerCollidingWithAnythingNew(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/world/phys/AABB;)Z"
        )
    )
    private boolean onCheckPlayerCollision(
        ServerGamePacketListenerImpl serverPlayNetworkHandler,
        LevelReader worldView,
        AABB box
    ) {
        if (IPGlobal.serverTeleportationManager.isJustTeleported(player, 100)) {
            return false;
        }
        if (((IEEntity) player).getCollidingPortal() != null) {
            return false;
        }
        boolean portalsNearby = IPMcHelper.getNearbyPortals(
            player,
            16
        ).findAny().isPresent();
        if (portalsNearby) {
            return false;
        }
        return isPlayerCollidingWithAnythingNew(worldView, box);
    }
    
    @Inject(
        method = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;handleAcceptTeleportPacket(Lnet/minecraft/network/protocol/game/ServerboundAcceptTeleportationPacket;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onOnTeleportConfirm(ServerboundAcceptTeleportationPacket packet, CallbackInfo ci) {
        if (awaitingPositionFromClient == null) {
            ci.cancel();
        }
    }
    
    //do not reject move when player is riding and entering portal
    //the client packet is not validated (validating it needs dimension info in packet)
    @Inject(
        method = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;handleMoveVehicle(Lnet/minecraft/network/protocol/game/ServerboundMoveVehiclePacket;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;containsInvalidValues(DDDFF)Z"
        ),
        cancellable = true
    )
    private void onOnVehicleMove(ServerboundMoveVehiclePacket packet, CallbackInfo ci) {
        if (IPGlobal.serverTeleportationManager.isJustTeleported(player, 40)) {
            Entity entity = this.player.getRootVehicle();
            
            if (entity != player) {
                double currX = entity.getX();
                double currY = entity.getY();
                double currZ = entity.getZ();
                
                double newX = packet.getX();
                double newY = packet.getY();
                double newZ = packet.getZ();
                
                if (entity.position().distanceToSqr(
                    newX, newY, newZ
                ) < 256) {
                    float yaw = packet.getYRot();
                    float pitch = packet.getXRot();
                    
                    entity.absMoveTo(newX, newY, newZ, yaw, pitch);
                    
                    this.player.getLevel().getChunkSource().move(this.player);
                    
                    clientVehicleIsFloating = false;
                    vehicleLastGoodX = entity.getX();
                    vehicleLastGoodY = entity.getY();
                    vehicleLastGoodZ = entity.getZ();
                }
            }
            
            ci.cancel();
        }
    }
    
    private static boolean shouldAcceptDubiousMovement(ServerPlayer player) {
        if (IPGlobal.serverTeleportationManager.isJustTeleported(player, 100)) {
            return true;
        }
        if (IPGlobal.looseMovementCheck) {
            return true;
        }
        if (((IEEntity) player).getCollidingPortal() != null) {
            return true;
        }
        boolean portalsNearby = IPMcHelper.getNearbyPortals(player, 16).findFirst().isPresent();
        if (portalsNearby) {
            return true;
        }
        return false;
    }
    
    @Override
    public void cancelTeleportRequest() {
        awaitingPositionFromClient = null;
    }
}
