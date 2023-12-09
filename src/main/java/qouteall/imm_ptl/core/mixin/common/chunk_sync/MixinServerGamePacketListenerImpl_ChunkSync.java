package qouteall.imm_ptl.core.mixin.common.chunk_sync;

import net.minecraft.network.protocol.game.ServerboundChunkBatchReceivedPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.chunk_loading.ImmPtlChunkTracking;

@Mixin(ServerGamePacketListenerImpl.class)
public class MixinServerGamePacketListenerImpl_ChunkSync {
    @Shadow public ServerPlayer player;
    
    @Inject(
        method = "handleChunkBatchReceived",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/PlayerChunkSender;onChunkBatchReceivedByClient(F)V"
        )
    )
    private void redirectOnChunkBatchReceivedByClient(
        ServerboundChunkBatchReceivedPacket packet, CallbackInfo ci
    ) {
        ImmPtlChunkTracking.getPlayerInfo(player)
            .onChunkBatchReceivedByClient(packet.desiredChunksPerTick());
    }
    
}
