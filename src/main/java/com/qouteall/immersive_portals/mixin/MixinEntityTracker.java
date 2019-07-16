package com.qouteall.immersive_portals.mixin;

import com.qouteall.immersive_portals.chunk_loading.RedirectedMessageManager;
import net.minecraft.entity.Entity;
import net.minecraft.network.Packet;
import net.minecraft.server.network.EntityTrackerEntry;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

//NOTE must redirect all packets about entities
@Mixin(targets = "net.minecraft.server.world.ThreadedAnvilChunkStorage$EntityTracker")
public class MixinEntityTracker {
    @Shadow
    @Final
    private EntityTrackerEntry entry;
    @Shadow
    @Final
    private Entity entity;
    
    @Redirect(
        method = "Lnet/minecraft/server/world/ThreadedAnvilChunkStorage$EntityTracker;sendToOtherNearbyPlayers(Lnet/minecraft/network/Packet;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerPlayNetworkHandler;sendPacket(Lnet/minecraft/network/Packet;)V"
        )
    )
    private void onSendToOtherNearbyPlayers(
        ServerPlayNetworkHandler serverPlayNetworkHandler,
        Packet<?> packet_1
    ) {
        serverPlayNetworkHandler.sendPacket(
            RedirectedMessageManager.createRedirectedMessage(
                entity.dimension,
                packet_1
            )
        );
    }
    
    @Redirect(
        method = "Lnet/minecraft/server/world/ThreadedAnvilChunkStorage$EntityTracker;sendToNearbyPlayers(Lnet/minecraft/network/Packet;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerPlayNetworkHandler;sendPacket(Lnet/minecraft/network/Packet;)V"
        )
    )
    private void onSendToNearbyPlayers(
        ServerPlayNetworkHandler serverPlayNetworkHandler,
        Packet<?> packet_1
    ) {
        serverPlayNetworkHandler.sendPacket(
            RedirectedMessageManager.createRedirectedMessage(
                entity.dimension,
                packet_1
            )
        );
    }
}
