package com.qouteall.immersive_portals.mixin.common.entity_sync;

import com.qouteall.immersive_portals.chunk_loading.EntitySync;
import net.minecraft.entity.Entity;
import net.minecraft.network.Packet;
import net.minecraft.server.network.EntityTrackerEntry;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

//NOTE must redirect all packets about entities
@Mixin(EntityTrackerEntry.class)
public abstract class MixinEntityTrackerEntry {
    @Shadow
    @Final
    private Entity entity;
    
    @Shadow
    public abstract void sendPackets(Consumer<Packet<?>> consumer_1);
    
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
        EntitySync.sendRedirectedPacket(serverPlayNetworkHandler, packet_1, entity.world.getRegistryKey());
    }
    
    @Inject(
        method = "startTracking",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/EntityTrackerEntry;sendPackets(Ljava/util/function/Consumer;)V"
        )
    )
    private void injectSendpacketsOnStartTracking(ServerPlayerEntity player, CallbackInfo ci) {
        this.sendPackets(packet -> EntitySync.sendRedirectedPacket(player.networkHandler, packet, entity.world.getRegistryKey()));
    }
    
    @Redirect(
        method = "startTracking",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/EntityTrackerEntry;sendPackets(Ljava/util/function/Consumer;)V"
        )
    )
    private void redirectSendPacketsOnStartTracking(
        EntityTrackerEntry entityTrackerEntry,
        Consumer<Packet<?>> sender
    ) {
        //nothing
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
        EntitySync.sendRedirectedPacket(serverPlayNetworkHandler, packet_1, entity.world.getRegistryKey());
    }
}
