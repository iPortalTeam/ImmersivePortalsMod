package qouteall.imm_ptl.core.mixin.common.entity_sync;

import net.minecraft.entity.Entity;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket;
import net.minecraft.server.network.EntityTrackerEntry;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
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

@Mixin(value = EntityTrackerEntry.class, priority = 1200)
public abstract class MixinEntityTrackerEntry implements IEEntityTrackerEntry {
    @Shadow
    @Final
    private Entity entity;
    
    @Shadow
    public abstract void sendPackets(Consumer<Packet<?>> consumer_1);
    
    @Shadow
    protected abstract void storeEncodedCoordinates();
    
    // make sure that the packet is being redirected
    @Inject(
        method = "tick",
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
    public void stopTracking(ServerPlayerEntity player) {
        IPCommonNetwork.withForceRedirect(
            ((ServerWorld) entity.world), () -> {
                entity.onStoppedTrackingBy(player);
                player.networkHandler.sendPacket(new EntitiesDestroyS2CPacket(entity.getId()));
            }
        );
    }
    
    /**
     * @author qouteall
     * @reason make incompat fail fast
     */
    @Overwrite
    public void startTracking(ServerPlayerEntity player) {
        IPCommonNetwork.withForceRedirect(
            ((ServerWorld) entity.world), () -> {
                ServerPlayNetworkHandler networkHandler = player.networkHandler;
                Objects.requireNonNull(networkHandler);
                this.sendPackets(networkHandler::sendPacket);
                this.entity.onStartedTrackingBy(player);
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
        method = "sendSyncPacket",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerPlayNetworkHandler;sendPacket(Lnet/minecraft/network/Packet;)V"
        )
    )
    private void onSendToWatcherAndSelf(
        ServerPlayNetworkHandler serverPlayNetworkHandler,
        Packet<?> packet_1
    ) {
        IPCommonNetwork.sendRedirectedPacket(serverPlayNetworkHandler, packet_1, entity.world.getRegistryKey());
    }
    
    @Override
    public void ip_updateTrackedEntityPosition() {
        storeEncodedCoordinates();
    }
}
