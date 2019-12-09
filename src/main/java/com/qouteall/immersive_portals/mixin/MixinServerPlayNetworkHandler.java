package com.qouteall.immersive_portals.mixin;

import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.ducks.IEPlayerMoveC2SPacket;
import com.qouteall.immersive_portals.ducks.IEPlayerPositionLookS2CPacket;
import com.qouteall.immersive_portals.ducks.IEServerPlayNetworkHandler;
import com.qouteall.immersive_portals.portal.Portal;
import net.minecraft.client.network.packet.PlayerPositionLookS2CPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.packet.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;
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
    protected abstract boolean method_20630(WorldView worldView_1);
    
    //do not process move packet when client dimension and server dimension are not synced
    @Inject(
        method = "Lnet/minecraft/server/network/ServerPlayNetworkHandler;onPlayerMove(Lnet/minecraft/server/network/packet/PlayerMoveC2SPacket;)V",
        at = @At(
            value = "INVOKE",
            shift = At.Shift.AFTER,
            target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/server/world/ServerWorld;)V"
        ),
        cancellable = true
    )
    private void onProcessMovePacket(PlayerMoveC2SPacket packet, CallbackInfo ci) {
        DimensionType packetDimension = ((IEPlayerMoveC2SPacket) packet).getPlayerDimension();
        
        assert packetDimension != null;
        
        if (player.dimension != packetDimension) {
            ci.cancel();
        }
    }
    
    /**
     * make PlayerPositionLookS2CPacket contain dimension data
     *
     * @author qouteall
     */
    @Overwrite
    public void teleportRequest(
        double double_1,
        double double_2,
        double double_3,
        float float_1,
        float float_2,
        Set<PlayerPositionLookS2CPacket.Flag> set_1
    ) {
        Helper.log(String.format("request teleport %s in %s to %s %s %s",
            player.getName().asString(),
            player.dimension,
            double_1, double_2, double_3
        ));
    
        double double_4 = set_1.contains(PlayerPositionLookS2CPacket.Flag.X) ? this.player.getX() : 0.0D;
        double double_5 = set_1.contains(PlayerPositionLookS2CPacket.Flag.Y) ? this.player.getY() : 0.0D;
        double double_6 = set_1.contains(PlayerPositionLookS2CPacket.Flag.Z) ? this.player.getZ() : 0.0D;
        float float_3 = set_1.contains(PlayerPositionLookS2CPacket.Flag.Y_ROT) ? this.player.yaw : 0.0F;
        float float_4 = set_1.contains(PlayerPositionLookS2CPacket.Flag.X_ROT) ? this.player.pitch : 0.0F;
        this.requestedTeleportPos = new Vec3d(double_1, double_2, double_3);
        if (++this.requestedTeleportId == Integer.MAX_VALUE) {
            this.requestedTeleportId = 0;
        }
        
        this.teleportRequestTick = this.ticks;
        this.player.setPositionAnglesAndUpdate(double_1, double_2, double_3, float_1, float_2);
        PlayerPositionLookS2CPacket packet_1 = new PlayerPositionLookS2CPacket(
            double_1 - double_4,
            double_2 - double_5,
            double_3 - double_6,
            float_1 - float_3,
            float_2 - float_4,
            set_1,
            this.requestedTeleportId
        );
        ((IEPlayerPositionLookS2CPacket) packet_1).setPlayerDimension(player.dimension);
        this.player.networkHandler.sendPacket(packet_1);
    }
    
    //server will check the collision when receiving position packet from client
    //we treat collision specially when player is halfway through a portal
    @Redirect(
        method = "onPlayerMove",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerPlayNetworkHandler;method_20630(Lnet/minecraft/world/WorldView;)Z"
        )
    )
    private boolean onCheckPlayerCollision(
        ServerPlayNetworkHandler serverPlayNetworkHandler,
        WorldView worldView_1
    ) {
        boolean portalsNearby = !player.world.getEntities(
            Portal.class,
            player.getBoundingBox().expand(4),
            e -> true
        ).isEmpty();
        if (portalsNearby) {
            return true;
        }
        return method_20630(worldView_1);
    }
    
    @Override
    public void cancelTeleportRequest() {
        requestedTeleportPos = null;
    }
}
