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
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
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
    @Inject(method = "markChunkPendingToSend(Lnet/minecraft/world/level/chunk/LevelChunk;)V",
        at = @At("HEAD"), cancellable = true)
    public void markChunkPendingToSend(CallbackInfo ci) {
        ci.cancel();
    }
    
    /**
     * @author qouteall
     * @reason see class comment
     */
    @Inject(method = "dropChunk(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/level/ChunkPos;)V",
        at = @At("HEAD"), cancellable = true)
    public void dropChunk(CallbackInfo ci) {
        ci.cancel();
    }
    
    /**
     * @author qouteall
     * @reason see class comment
     */
    @Inject(method = "sendNextChunks(Lnet/minecraft/server/level/ServerPlayer;)V",
        at = @At("HEAD"), cancellable = true)
    public void sendNextChunks(ServerPlayer serverPlayer, CallbackInfo ci) {
        ImmPtlChunkTracking.getPlayerInfo(serverPlayer).doChunkSending(serverPlayer);
        ci.cancel();
    }
    
    /**
     * @author qouteall
     * @reason see class comment
     */
    @Inject(method = "onChunkBatchReceivedByClient(F)V",
        at = @At("HEAD"), cancellable = true)
    public void onChunkBatchReceivedByClient(CallbackInfo ci) {
        ci.cancel();
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
