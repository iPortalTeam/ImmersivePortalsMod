package com.qouteall.immersive_portals.mixin.common;

import com.qouteall.hiding_in_the_bushes.MyNetwork;
import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.chunk_loading.NewChunkTrackingGraph;
import com.qouteall.immersive_portals.portal.global_portals.GlobalPortalStorage;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(PlayerManager.class)
public class MixinPlayerManager {
    @Shadow
    @Final
    private List<ServerPlayerEntity> players;
    
    @Shadow
    @Final
    private MinecraftServer server;
    
    @Inject(
        method = "onPlayerConnect",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/packet/s2c/play/GameJoinS2CPacket;<init>(ILnet/minecraft/world/GameMode;Lnet/minecraft/world/GameMode;JZLjava/util/Set;Lnet/minecraft/util/registry/DynamicRegistryManager$Impl;Lnet/minecraft/world/dimension/DimensionType;Lnet/minecraft/util/registry/RegistryKey;IIZZZZ)V"
        )
    )
    private void onConnectionEstablished(
        ClientConnection connection,
        ServerPlayerEntity player,
        CallbackInfo ci
    ) {
        player.networkHandler.sendPacket(MyNetwork.createDimSync());
    }
    
    @Inject(method = "sendWorldInfo", at = @At("RETURN"))
    private void onSendWorldInfo(ServerPlayerEntity player, ServerWorld world, CallbackInfo ci) {
        if (!Global.serverTeleportationManager.isFiringMyChangeDimensionEvent) {
            GlobalPortalStorage.onPlayerLoggedIn(player);
        }
    }
    
    @Inject(method = "onPlayerConnect", at = @At("TAIL"))
    private void onOnPlayerConnect(ClientConnection connection, ServerPlayerEntity player, CallbackInfo ci) {
        NewChunkTrackingGraph.updateForPlayer(player);
    }
    
    //with redirection
    @Inject(
        method = "sendToDimension",
        at = @At("HEAD"),
        cancellable = true
    )
    public void sendToDimension(Packet<?> packet, RegistryKey<World> dimension, CallbackInfo ci) {
        for (ServerPlayerEntity player : players) {
            if (player.world.getRegistryKey() == dimension) {
                player.networkHandler.sendPacket(
                    MyNetwork.createRedirectedMessage(
                        dimension,
                        packet
                    )
                );
            }
        }
        
        ci.cancel();
    }
}
