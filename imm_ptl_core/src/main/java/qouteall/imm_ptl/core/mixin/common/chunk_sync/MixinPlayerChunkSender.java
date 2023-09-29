package qouteall.imm_ptl.core.mixin.common.chunk_sync;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.PlayerChunkSender;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * Disable the functionality of this class.
 * Because its implementation is based on single-dimension loaded and near-loading-only assumption.
 */
@Mixin(PlayerChunkSender.class)
public class MixinPlayerChunkSender {
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
        throw new RuntimeException("This method should not be called");
    }
}
