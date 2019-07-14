package com.qouteall.immersive_portals.mixin;

import com.qouteall.immersive_portals.Globals;
import com.qouteall.immersive_portals.chunk_loading.DimensionalChunkPos;
import net.minecraft.network.Packet;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public class MixinServerPlayerEntity {
    @Inject(
        method = "Lnet/minecraft/server/network/ServerPlayerEntity;sendInitialChunkPackets(Lnet/minecraft/util/math/ChunkPos;Lnet/minecraft/network/Packet;Lnet/minecraft/network/Packet;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onSendChunkDataPackets(
        ChunkPos chunkPos_1,
        Packet<?> packet_1,
        Packet<?> packet_2,
        CallbackInfo ci
    ) {
        ServerPlayerEntity this_ = (ServerPlayerEntity) (Object) this;
        boolean isWatching = Globals.chunkTracker.isPlayerWatchingChunk(
            this_,
            new DimensionalChunkPos(
                this_.dimension,
                chunkPos_1
            )
        );
        if (isWatching) {
            ci.cancel();
        }
    }
    
    @Inject(
        method = "Lnet/minecraft/server/network/ServerPlayerEntity;sendUnloadChunkPacket(Lnet/minecraft/util/math/ChunkPos;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onSendUnloadChunkPacket(ChunkPos chunkPos_1, CallbackInfo ci) {
        ServerPlayerEntity this_ = (ServerPlayerEntity) (Object) this;
        DimensionalChunkPos dimensionalChunkPos = new DimensionalChunkPos(
            this_.dimension,
            chunkPos_1
        );
        
        Globals.chunkDataSyncManager.manageToSendUnloadPacket(
            this_, dimensionalChunkPos
        );
        
        ci.cancel();
    }
}
