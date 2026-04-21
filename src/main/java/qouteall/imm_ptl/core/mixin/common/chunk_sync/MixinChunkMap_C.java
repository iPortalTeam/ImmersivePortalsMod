package qouteall.imm_ptl.core.mixin.common.chunk_sync;

import net.minecraft.server.level.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.chunk_loading.ImmPtlChunkTracking;
import qouteall.imm_ptl.core.chunk_loading.PlayerChunkLoading;
import qouteall.imm_ptl.core.ducks.IEChunkMap;

import java.util.HashSet;
import java.util.Set;

@Mixin(value = ChunkMap.class, priority = 1100)
public abstract class MixinChunkMap_C implements IEChunkMap {
    
    @Shadow
    @Final
    private ServerLevel level;
    
    @Shadow
    protected abstract ChunkHolder getVisibleChunkIfPresent(long long_1);
    
    @Shadow
    @Final
    private ThreadedLevelLightEngine lightEngine;
    
    @Shadow
    abstract int getPlayerViewDistance(ServerPlayer serverPlayer);
    
    @Override
    public int ip_getPlayerViewDistance(ServerPlayer player) {
        return getPlayerViewDistance(player);
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
    public ChunkHolder ip_getChunkHolder(long chunkPosLong) {
        return getVisibleChunkIfPresent(chunkPosLong);
    }

    /**
     * @author daimond113
     * @reason Maintain compatibility with APIs such as Fabric's PlayerLookup
     */
    @Overwrite
    public boolean isChunkTracked(ServerPlayer player, int x, int z) {
        return ImmPtlChunkTracking.isPlayerWatchingChunk(
            player,
            level.dimension(),
            x,
            z
        );
    }

    @Unique
    private Set<ServerPlayer> ip_getPlayersInChunk(ChunkPos pos, boolean boundaryOnly) {
        return new HashSet<>(ImmPtlChunkTracking.getPlayersViewingChunk(
            level.dimension(),
            pos.x, pos.z,
            boundaryOnly
        ));
    }

    @Redirect(
        method = "getPlayersCloseForSpawning",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/PlayerMap;getAllPlayers()Ljava/util/Set;"
        )
    )
    private Set<ServerPlayer> redirectGetPlayersCloseForSpawning(PlayerMap instance, ChunkPos pos) {
        return ip_getPlayersInChunk(pos, false);
    }

    @Redirect(
        method = "getPlayers",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/PlayerMap;getAllPlayers()Ljava/util/Set;"
        )
    )
    private Set<ServerPlayer> redirectGetPlayers(PlayerMap instance, ChunkPos pos, boolean boundaryOnly) {
        return ip_getPlayersInChunk(pos, boundaryOnly);
    }

    /**
     * packets will be sent on {@link PlayerChunkLoading}
     */
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
        ImmPtlChunkTracking.onChunkProvidedDeferred(chunk);
    }
}
