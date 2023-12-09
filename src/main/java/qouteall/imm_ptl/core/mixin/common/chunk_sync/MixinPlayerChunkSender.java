package qouteall.imm_ptl.core.mixin.common.chunk_sync;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.PlayerChunkSender;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import qouteall.imm_ptl.core.chunk_loading.ImmPtlChunkTracking;

/**
 * Disable the functionality of this class.
 * Because its implementation is based on single-dimension loaded and near-loading-only assumption.
 */
@Mixin(PlayerChunkSender.class)
public class MixinPlayerChunkSender {
    @Shadow @Final private static Logger LOGGER;
    
    /**
     * @author qouteall
     * @reason see class comment
     */
    @Overwrite
    public void markChunkPendingToSend(LevelChunk levelChunk) {
    
    }
    
    /**
     * @author qouteall
     * @reason see class comment
     */
    @Overwrite
    public void dropChunk(ServerPlayer serverPlayer, ChunkPos chunkPos) {
    
    }
    
    /**
     * @author qouteall
     * @reason see class comment
     */
    @Overwrite
    public void sendNextChunks(ServerPlayer serverPlayer) {
        ImmPtlChunkTracking.getPlayerInfo(serverPlayer).doChunkSending(serverPlayer);
    }
    
    /**
     * @author qouteall
     * @reason see class comment
     */
    @Overwrite
    public void onChunkBatchReceivedByClient(float f) {
    
    }
    
    /**
     * @author qouteall
     * @reason see class comment
     */
    @Overwrite
    public boolean isPending(long l) {
        LOGGER.error("This should not be called", new Throwable());
        return false;
    }
}
