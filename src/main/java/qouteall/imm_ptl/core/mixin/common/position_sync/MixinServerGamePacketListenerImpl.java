package qouteall.imm_ptl.core.mixin.common.position_sync;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.ducks.IEEntity;
import qouteall.imm_ptl.core.ducks.IEPlayerMoveC2SPacket;
import qouteall.imm_ptl.core.ducks.IEPlayerPositionLookS2CPacket;
import qouteall.imm_ptl.core.ducks.IEServerPlayNetworkHandler;
import qouteall.imm_ptl.core.mc_utils.ServerTaskList;
import qouteall.imm_ptl.core.miscellaneous.IPVanillaCopy;
import qouteall.imm_ptl.core.teleportation.ServerTeleportationManager;
import qouteall.q_misc_util.my_util.CountDownInt;

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
    private double vehicleFirstGoodX;
    
    @Shadow
    private double vehicleFirstGoodY;
    
    @Shadow
    private double vehicleFirstGoodZ;
    
    @Shadow
    private Entity lastVehicle;
    
    @Shadow
    private boolean clientVehicleIsFloating;
    
    @Shadow
    @Final
    static Logger LOGGER;
    
    @Shadow
    public abstract ServerPlayer getPlayer();
    
    @Shadow
    private boolean clientIsFloating;
    
    @Unique
    private static final CountDownInt LOG_LIMIT = new CountDownInt(20);
    
    @Unique
    private int ip_wrongMovePacketCount = 0;
    
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
        ResourceKey<Level> packetDimension = ((IEPlayerMoveC2SPacket) packet).ip_getPlayerDimension();
        
        if (packetDimension == null) {
            // this actually never happens, because the vanilla client will disconnect immediately
            // when receiving the position sync packet that has the extra dimension field
            LOGGER.error("Player move packet is missing dimension info. Maybe the player client doesn't have ImmPtl");
            ServerTaskList.of(player.server).addTask(() -> {
                player.connection.disconnect(Component.literal(
                    "The client does not have Immersive Portals mod"
                ));
                return true;
            });
            return;
        }
        
        if (player.level().dimension() != packetDimension) {
            if (LOG_LIMIT.tryDecrement()) {
                LOGGER.info(
                    "[ImmPtl] Ignoring player move packet {} {}",
                    player, packetDimension.location()
                );
            }
            
            ip_wrongMovePacketCount += 1;
            
            if (ip_wrongMovePacketCount > 200) {
                LOGGER.info(
                    "[ImmPtl] Force move player {} {} {}",
                    player, player.level().dimension().location(), player.position()
                );
                ServerTeleportationManager.of(player.server).forceTeleportPlayer(
                    player, player.level().dimension(), player.position()
                );
                ip_wrongMovePacketCount = 0;
            }
            
            ci.cancel();
        }
        else {
            ip_wrongMovePacketCount = 0;
        }
    }
    
    /**
     * @reason make PlayerPositionLookS2CPacket contain dimension data and do some special handling
     * @author qouteall
     */
    @Overwrite
    @IPVanillaCopy
    public void teleport(
        double x, double y, double z, float yaw, float pitch,
        Set<RelativeMovement> relativeAttrs
    ) {
        // it may request teleport while this.player is marked removed during respawn
        
        if (player.getRemovalReason() != null) {
            LOGGER.error(
                "[ImmPtl] Tries to send player pos packet to a removed player {}",
                player, new Throwable()
            );
            return;
        }
        
        double xBase = relativeAttrs.contains(RelativeMovement.X) ? this.player.getX() : 0.0;
        double yBase = relativeAttrs.contains(RelativeMovement.Y) ? this.player.getY() : 0.0;
        double zBase = relativeAttrs.contains(RelativeMovement.Z) ? this.player.getZ() : 0.0;
        float yRotBase = relativeAttrs.contains(RelativeMovement.Y_ROT) ? this.player.getYRot() : 0.0f;
        float xRotBase = relativeAttrs.contains(RelativeMovement.X_ROT) ? this.player.getXRot() : 0.0f;
        
        this.awaitingPositionFromClient = new Vec3(x, y, z);
        if (++this.awaitingTeleport == Integer.MAX_VALUE) {
            this.awaitingTeleport = 0;
        }
        
        this.awaitingTeleportTime = this.tickCount;
        this.player.absMoveTo(x, y, z, yaw, pitch);
        ClientboundPlayerPositionPacket lookPacket = new ClientboundPlayerPositionPacket(
            x - xBase, y - yBase, z - zBase,
            yaw - yRotBase, pitch - xRotBase,
            relativeAttrs, this.awaitingTeleport
        );
        
        ((IEPlayerPositionLookS2CPacket) lookPacket).ip_setPlayerDimension(player.level().dimension());
        
        this.player.connection.send(lookPacket);
    }
    
    @Inject(
        method = "isPlayerCollidingWithAnythingNew", at = @At("HEAD"), cancellable = true
    )
    private void onIsPlayerCollidingWithAnythingNew(
        LevelReader level, AABB playerBB, double newX, double newY, double newZ, CallbackInfoReturnable<Boolean> cir
    ) {
        if (!IPGlobal.crossPortalCollision) {
            return;
        }
        
        AABB activePlayerBB = ((IEEntity) player).ip_getActiveCollisionBox(playerBB);
        
        if (activePlayerBB == null) {
            cir.setReturnValue(false);
            return;
        }
        
        AABB newBB = this.player.getBoundingBox().move(
            newX - this.player.getX(), newY - this.player.getY(), newZ - this.player.getZ()
        );
        
        AABB activeNewBB = ((IEEntity) player).ip_getActiveCollisionBox(newBB);
        
        if (activeNewBB == null) {
            cir.setReturnValue(false);
            return;
        }
        
        Iterable<VoxelShape> newBBCollisions =
            level.getCollisions(this.player, activeNewBB.deflate(1.0E-5F));
        
        VoxelShape activePlayerBBShape = Shapes.create(activePlayerBB.deflate(1.0E-5F));
        
        for (VoxelShape shape : newBBCollisions) {
            if (!Shapes.joinIsNotEmpty(shape, activePlayerBBShape, BooleanOp.AND)) {
                // when a new collision box does not intersect with old bounding box
                // it's considered as colliding with something new
                cir.setReturnValue(true);
                return;
            }
        }
        
        cir.setReturnValue(false);
    }
    
    // don't get recognized as floating when standing on blocks on other side of portal
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        if (((IEEntity) player).ip_isRecentlyCollidingWithPortal()) {
            clientIsFloating = false;
        }
    }
    
}
