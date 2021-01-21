package com.qouteall.immersive_portals.mixin.common;

import com.qouteall.hiding_in_the_bushes.MyNetwork;
import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.chunk_loading.NewChunkTrackingGraph;
import com.qouteall.immersive_portals.portal.global_portals.GlobalPortalStorage;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
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
    
    /**
     * @author qoutall
     * mostly for sound events
     */
    @Overwrite
    public void sendToAround(
        @Nullable PlayerEntity excludingPlayer,
        double x, double y, double z, double distance,
        RegistryKey<World> dimension, Packet<?> packet
    ) {
        ChunkPos chunkPos = new ChunkPos(new BlockPos(new Vec3d(x, y, z)));
        
        NewChunkTrackingGraph.getPlayersViewingChunk(
            dimension, chunkPos.x, chunkPos.z
        ).filter(playerEntity -> NewChunkTrackingGraph.isPlayerWatchingChunkWithinRaidus(
            playerEntity, dimension, chunkPos.x, chunkPos.z, (int) distance + 16
        )).forEach(playerEntity -> {
            if (playerEntity != excludingPlayer) {
                playerEntity.networkHandler.sendPacket(MyNetwork.createRedirectedMessage(
                    dimension, packet
                ));
            }
        });
    }
}
