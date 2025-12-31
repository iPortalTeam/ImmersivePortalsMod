package qouteall.imm_ptl.core.mixin.common.chunk_sync;

import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.logging.LogUtils;

import net.minecraft.network.protocol.game.ServerboundChunkBatchReceivedPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import qouteall.imm_ptl.core.chunk_loading.ImmPtlChunkTracking;

@Mixin(ServerGamePacketListenerImpl.class)
public class MixinServerGamePacketListenerImpl_ChunkSync {

    @Shadow public ServerPlayer player;

    @Unique private static final Logger LOGGER = LogUtils.getLogger();
    @Unique private int ip_dbg_counter;

    @Inject(
        method = "handleChunkBatchReceived",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/PlayerChunkSender;onChunkBatchReceivedByClient(F)V"
        )
    )
    private void ip_onChunkBatchReceived(ServerboundChunkBatchReceivedPacket packet, CallbackInfo ci) {

        // log “rate-limited” (1 ogni 64 chiamate per quel player)
        if ( (ip_dbg_counter++ & 63) == 0 ) {
            LOGGER.info("[ImmPtlDbg] handleChunkBatchReceived desiredChunksPerTick={} player={}",
                packet.desiredChunksPerTick(),
                player.getName().getString()
            );
        }

        ImmPtlChunkTracking.getPlayerInfo(player)
            .onChunkBatchReceivedByClient(packet.desiredChunksPerTick());
    }
}
