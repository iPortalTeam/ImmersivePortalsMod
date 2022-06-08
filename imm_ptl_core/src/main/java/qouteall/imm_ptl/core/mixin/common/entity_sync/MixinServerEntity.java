package qouteall.imm_ptl.core.mixin.common.entity_sync;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.VecDeltaCodec;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.ducks.IEEntityTrackerEntry;
import qouteall.imm_ptl.core.network.IPCommonNetwork;

import java.util.Objects;
import java.util.function.Consumer;

@Mixin(value = ServerEntity.class, priority = 1200)
public abstract class MixinServerEntity implements IEEntityTrackerEntry {
    @Shadow
    @Final
    private Entity entity;
    
    @Shadow
    public abstract void sendPairingData(Consumer<Packet<?>> consumer_1);
    
    @Shadow @Final private VecDeltaCodec positionCodec;
    
    // make sure that the packet is being redirected
    @Inject(
        method = "Lnet/minecraft/server/level/ServerEntity;sendChanges()V",
        at = @At("HEAD")
    )
    private void onTick(CallbackInfo ci) {
        IPCommonNetwork.validateForceRedirecting();
    }
    
    /**
     * @author qouteall
     * @reason make incompat fail fast
     */
    @Overwrite
    public void removePairing(ServerPlayer player) {
        IPCommonNetwork.withForceRedirect(
            ((ServerLevel) entity.level), () -> {
                entity.stopSeenByPlayer(player);
                player.connection.send(new ClientboundRemoveEntitiesPacket(entity.getId()));
            }
        );
    }
    
    /**
     * @author qouteall
     * @reason make incompat fail fast
     */
    @Overwrite
    public void addPairing(ServerPlayer player) {
        IPCommonNetwork.withForceRedirect(
            ((ServerLevel) entity.level), () -> {
                ServerGamePacketListenerImpl networkHandler = player.connection;
                Objects.requireNonNull(networkHandler);
                this.sendPairingData(networkHandler::send);
                this.entity.startSeenByPlayer(player);
            }
        );
    }

//    @Inject(
//        method = "startTracking",
//        at = @At("HEAD")
//    )
//    private void onStartTracking(ServerPlayerEntity player, CallbackInfo ci) {
//        CommonNetwork.validateForceRedirecting();
//    }
//
//    @Inject(
//        method = "stopTracking", at = @At("HEAD")
//    )
//    private void onStopTracking(ServerPlayerEntity player, CallbackInfo ci) {
//        CommonNetwork.validateForceRedirecting();
//    }
    
    @Redirect(
        method = "Lnet/minecraft/server/level/ServerEntity;broadcastAndSend(Lnet/minecraft/network/protocol/Packet;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;send(Lnet/minecraft/network/protocol/Packet;)V"
        )
    )
    private void onSendToWatcherAndSelf(
        ServerGamePacketListenerImpl serverPlayNetworkHandler,
        Packet<?> packet_1
    ) {
        IPCommonNetwork.sendRedirectedPacket(serverPlayNetworkHandler, packet_1, entity.level.dimension());
    }
    
    @Override
    public void ip_updateTrackedEntityPosition() {
        positionCodec.setBase(entity.trackingPosition());
    }
}
