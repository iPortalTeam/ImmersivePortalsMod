package qouteall.imm_ptl.core.mixin.common.position_sync;

import qouteall.imm_ptl.core.IPGlobal;
import qouteall.q_misc_util.Helper;
import qouteall.imm_ptl.core.IPMcHelper;
import qouteall.imm_ptl.core.ducks.IEEntity;
import qouteall.imm_ptl.core.ducks.IEPlayerMoveC2SPacket;
import qouteall.imm_ptl.core.ducks.IEPlayerPositionLookS2CPacket;
import qouteall.imm_ptl.core.ducks.IEServerPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(value = ServerPlayNetworkHandler.class, priority = 900)
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
    private double updatedRiddenX;
    
    @Shadow
    private double updatedRiddenY;
    
    @Shadow
    private double updatedRiddenZ;
    
    @Shadow
    protected abstract boolean isHost();
    
    @Shadow
    protected abstract boolean isPlayerNotCollidingWithBlocks(WorldView worldView, Box box);
    
    @Shadow
    public abstract void disconnect(Text reason);
    
    @Shadow
    private double lastTickRiddenX;
    
    @Shadow
    private double lastTickRiddenY;
    
    @Shadow
    private double lastTickRiddenZ;
    
    @Shadow
    @Final
    private static Logger LOGGER;
    
    @Shadow
    @Final
    public ClientConnection connection;
    
    @Shadow
    @Final
    private MinecraftServer server;
    
    @Shadow
    private Entity topmostRiddenEntity;
    
    @Shadow
    private boolean vehicleFloating;
    
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
        
        if (packetDimension == null) {
            Helper.err("Player move packet is missing dimension info. Maybe the player client doesn't have IP");
            IPGlobal.serverTaskList.addTask(() -> {
                player.networkHandler.disconnect(new LiteralText(
                    "The client does not have Immersive Portals mod"
                ));
                return true;
            });
            return;
        }
        
        if (IPGlobal.serverTeleportationManager.isJustTeleported(player, 100)) {
            cancelTeleportRequest();
        }
        
        if (player.world.getRegistryKey() != packetDimension) {
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
        method = "onPlayerMove",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerPlayNetworkHandler;isHost()Z"
        )
    )
    private boolean redirectIsServerOwnerOnPlayerMove(ServerPlayNetworkHandler serverPlayNetworkHandler) {
        if (shouldAcceptDubiousMovement(player)) {
            return true;
        }
        return isHost();
    }
    
    @Redirect(
        method = "onPlayerMove",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerPlayerEntity;isInTeleportationState()Z"
        ),
        require = 0 // don't crash with carpet
    )
    private boolean redirectIsInTeleportationState(ServerPlayerEntity player) {
        if (shouldAcceptDubiousMovement(player)) {
            return true;
        }
        return player.isInTeleportationState();
    }
    
    /**
     * @reason make PlayerPositionLookS2CPacket contain dimension data and do some special handling
     * @author qouteall
     */
    @Overwrite
    public void requestTeleport(double x, double y, double z, float yaw, float pitch, Set<PlayerPositionLookS2CPacket.Flag> flags, boolean shouldDismount) {
        // it may request teleport while this.player is marked removed during respawn
        
        double d = flags.contains(PlayerPositionLookS2CPacket.Flag.X) ? this.player.getX() : 0.0D;
        double e = flags.contains(PlayerPositionLookS2CPacket.Flag.Y) ? this.player.getY() : 0.0D;
        double f = flags.contains(PlayerPositionLookS2CPacket.Flag.Z) ? this.player.getZ() : 0.0D;
        float g = flags.contains(PlayerPositionLookS2CPacket.Flag.Y_ROT) ? this.player.getYaw() : 0.0F;
        float h = flags.contains(PlayerPositionLookS2CPacket.Flag.X_ROT) ? this.player.getPitch() : 0.0F;
        this.requestedTeleportPos = new Vec3d(x, y, z);
        if (++this.requestedTeleportId == Integer.MAX_VALUE) {
            this.requestedTeleportId = 0;
        }

//        if (IPGlobal.serverTeleportationManager.isJustTeleported(player, 100)) {
//            Helper.err("Teleport request cancelled " + player.getName().asString());
//            return;
//        }
        
        this.teleportRequestTick = this.ticks;
        this.player.updatePositionAndAngles(x, y, z, yaw, pitch);
        PlayerPositionLookS2CPacket lookPacket = new PlayerPositionLookS2CPacket(x - d, y - e, z - f, yaw - g, pitch - h, flags, this.requestedTeleportId, shouldDismount);
        
        ((IEPlayerPositionLookS2CPacket) lookPacket).setPlayerDimension(player.world.getRegistryKey());
        
        this.player.networkHandler.sendPacket(lookPacket);
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
            target = "Lnet/minecraft/server/network/ServerPlayNetworkHandler;isMovementInvalid(DDDFF)Z"
        ),
        cancellable = true
    )
    private void onOnVehicleMove(VehicleMoveC2SPacket packet, CallbackInfo ci) {
        if (IPGlobal.serverTeleportationManager.isJustTeleported(player, 40)) {
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
                    
                    this.player.getWorld().getChunkManager().updatePosition(this.player);
                    
                    vehicleFloating = false;
                    updatedRiddenX = entity.getX();
                    updatedRiddenY = entity.getY();
                    updatedRiddenZ = entity.getZ();
                }
            }
            
            ci.cancel();
        }
    }
    
    private static boolean shouldAcceptDubiousMovement(ServerPlayerEntity player) {
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
        requestedTeleportPos = null;
    }
}
