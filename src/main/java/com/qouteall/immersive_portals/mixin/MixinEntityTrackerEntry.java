package com.qouteall.immersive_portals.mixin;

import com.qouteall.immersive_portals.MyNetwork;
import net.minecraft.entity.Entity;
import net.minecraft.network.Packet;
import net.minecraft.server.network.EntityTrackerEntry;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Consumer;

//NOTE must redirect all packets about entities
@Mixin(EntityTrackerEntry.class)
public abstract class MixinEntityTrackerEntry {
    @Shadow
    @Final
    private Entity entity;
    
    @Shadow
    public abstract void sendPackets(Consumer<Packet<?>> consumer_1);
    
    private void sendRedirectedMessage(
        ServerPlayNetworkHandler serverPlayNetworkHandler,
        Packet<?> packet_1
    ) {
        serverPlayNetworkHandler.sendPacket(
            MyNetwork.createRedirectedMessage(
                entity.dimension,
                packet_1
            )
        );
    }
    
    @Redirect(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerPlayNetworkHandler;sendPacket(Lnet/minecraft/network/Packet;)V"
        )
    )
    private void onSendPositionSyncPacket(
        ServerPlayNetworkHandler serverPlayNetworkHandler,
        Packet<?> packet_1
    ) {
        sendRedirectedMessage(serverPlayNetworkHandler, packet_1);
    }
    
    /**
     * @author qouteall
     * overwrite because method reference can not be redirected
     */
    @Overwrite
    public void startTracking(ServerPlayerEntity serverPlayerEntity_1) {
        ServerPlayNetworkHandler networkHandler = serverPlayerEntity_1.networkHandler;
        this.sendPackets(packet -> sendRedirectedMessage(networkHandler, packet));
        this.entity.onStartedTrackingBy(serverPlayerEntity_1);
        serverPlayerEntity_1.onStartedTracking(this.entity);
    }
    
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
        sendRedirectedMessage(serverPlayNetworkHandler, packet_1);
    }
}
