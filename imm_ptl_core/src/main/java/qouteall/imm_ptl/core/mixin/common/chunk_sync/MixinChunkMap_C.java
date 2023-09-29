package qouteall.imm_ptl.core.mixin.common.chunk_sync;

import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.ducks.IEThreadedAnvilChunkStorage;

@Mixin(value = ChunkMap.class, priority = 1100)
public abstract class MixinChunkMap_C implements IEThreadedAnvilChunkStorage {
    @Shadow
    private int viewDistance;
    
    @Shadow
    @Final
    private ServerLevel level;
    
    @Shadow
    protected abstract ChunkHolder getVisibleChunkIfPresent(long long_1);
    
    @Shadow
    @Final
    private ThreadedLevelLightEngine lightEngine;
    
    @Override
    public int ip_getWatchDistance() {
        return viewDistance;
    }
    
    @Override
    public ServerLevel ip_getWorld() {
        return level;
    }
    
    @Override
    public ThreadedLevelLightEngine ip_getLightingProvider() {
        return lightEngine;
    }
    
    @Override
    public ChunkHolder ip_getChunkHolder(long long_1) {
        return getVisibleChunkIfPresent(long_1);
    }
    
    // packets will be sent on ChunkDataSyncManager
    @Inject(
        method = "applyChunkTrackingView",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onUpdateChunkTracking(
        ServerPlayer serverPlayer, ChunkTrackingView chunkTrackingView, CallbackInfo ci
    ) {
        ci.cancel();
    }
    
    /**
     * @author qouteall
     * @reason
     */
    @Overwrite
    private void onChunkReadyToSend(LevelChunk chunk) {
        IPGlobal.chunkDataSyncManager.onChunkProvidedDeferred(chunk);
    }
}
