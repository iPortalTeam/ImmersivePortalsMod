package qouteall.imm_ptl.core.mixin.common.entity_sync;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.VecDeltaCodec;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.ducks.IEEntityTrackerEntry;
import qouteall.imm_ptl.core.network.PacketRedirection;
import qouteall.imm_ptl.core.portal.Portal;

@Mixin(value = ServerEntity.class, priority = 1200)
public abstract class MixinServerEntity implements IEEntityTrackerEntry {
    @Shadow
    @Final
    private Entity entity;
    
    @Shadow @Final private VecDeltaCodec positionCodec;
    
    @Shadow @Final private static Logger LOGGER;
    
    // make sure that the packet is being redirected
    @Inject(
        method = "Lnet/minecraft/server/level/ServerEntity;sendChanges()V",
        at = @At("HEAD")
    )
    private void onTick(CallbackInfo ci) {
        PacketRedirection.validateForceRedirecting();
    }
    
    @Redirect(
        method = "removePairing",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;send(Lnet/minecraft/network/protocol/Packet;)V"
        )
    )
    private void onSendRemoveEntityPacket(
        ServerGamePacketListenerImpl networkHandler,
        Packet packet
    ) {
        if (IPGlobal.entityUnloadDebug) {
            LOGGER.info("[Debug] Entity remove packet sent {}", entity);
        }
        PacketRedirection.sendRedirectedPacket(networkHandler, packet, entity.level().dimension());
    }
    
    @Redirect(
        method = "addPairing",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;send(Lnet/minecraft/network/protocol/Packet;)V"
        )
    )
    private void onSendAddEntityPacket(
        ServerGamePacketListenerImpl networkHandler,
        Packet packet
    ) {
        PacketRedirection.sendRedirectedPacket(networkHandler, packet, entity.level().dimension());
    }
    
    @Redirect(
        method = "Lnet/minecraft/server/level/ServerEntity;broadcastAndSend(Lnet/minecraft/network/protocol/Packet;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;send(Lnet/minecraft/network/protocol/Packet;)V"
        )
    )
    private void onSendToWatcherAndSelf(
        ServerGamePacketListenerImpl serverPlayNetworkHandler,
        Packet packet
    ) {
        PacketRedirection.sendRedirectedPacket(serverPlayNetworkHandler, packet, entity.level().dimension());
    }
    
    /**
     * It encodes position into 1/4096 units. That precision is not enough for portals.
     * The portal position is synced in {@link Portal#reloadAndSyncToClient()}
     */
    @Inject(
        method = "sendChanges",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onSendChanges(CallbackInfo ci) {
        if (entity instanceof Portal) {
            ci.cancel();
        }
    }
    
    @Override
    public void ip_updateTrackedEntityPosition() {
        positionCodec.setBase(entity.trackingPosition());
    }
}
