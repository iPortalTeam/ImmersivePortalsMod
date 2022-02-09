package qouteall.imm_ptl.core.mixin.common.chunk_sync;

import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ChunkTaskPriorityQueueSorter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.util.thread.ProcessorHandle;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.ducks.IEThreadedAnvilChunkStorage;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

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
    abstract void updatePlayerStatus(
        ServerPlayer serverPlayerEntity_1,
        boolean boolean_1
    );
    
    @Shadow
    @Final
    private Int2ObjectMap entityMap;
    
    @Shadow
    @Final
    private AtomicInteger tickingGenerated;
    
    @Shadow
    @Final
    private ProcessorHandle<ChunkTaskPriorityQueueSorter.Message<Runnable>> mainThreadMailbox;
    
    @Shadow
    @Nullable
    protected abstract CompoundTag readChunk(ChunkPos pos) throws IOException;
    
    @Shadow
    @Final
    private ThreadedLevelLightEngine lightEngine;
    
    @Override
    public int getWatchDistance() {
        return viewDistance;
    }
    
    @Override
    public ServerLevel getWorld() {
        return level;
    }
    
    @Override
    public ThreadedLevelLightEngine getLightingProvider() {
        return lightEngine;
    }
    
    @Override
    public ChunkHolder getChunkHolder_(long long_1) {
        return getVisibleChunkIfPresent(long_1);
    }
    
    @Override
    public boolean portal_isChunkGenerated(ChunkPos chunkPos) {
        if (level.getChunkSource().hasChunk(chunkPos.x, chunkPos.z)) {
            return true;
        }
        
        try {
            CompoundTag tag = readChunk(chunkPos);
            return tag != null;
        }
        catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * @author qouteall
     * @reason make mod incompatibility fail fast
     */
    @Overwrite
    private void playerLoadedChunk(
        ServerPlayer player, MutableObject<ClientboundLevelChunkWithLightPacket> cachedDataPacket, LevelChunk chunk
    ) {
        //chunk data packets will be sent on ChunkDataSyncManager
    }
    
    /**
     * @author qouteall
     * @reason make mod incompatibility fail fast
     */
    @Overwrite
    public void updateChunkTracking(ServerPlayer player, ChunkPos pos, MutableObject<ClientboundLevelChunkWithLightPacket> mutableObject, boolean oldWithinViewDistance, boolean newWithinViewDistance) {
        // packets will be sent on ChunkDataSyncManager
    }
    
    //do my packet sending
    @Inject(
        method = "Lnet/minecraft/server/level/ChunkMap;prepareTickingChunk(Lnet/minecraft/server/level/ChunkHolder;)Ljava/util/concurrent/CompletableFuture;",
        at = @At("RETURN"),
        cancellable = true
    )
    private void onCreateTickingFuture(
        ChunkHolder chunkHolder,
        CallbackInfoReturnable<CompletableFuture<Either<LevelChunk, ChunkHolder.ChunkLoadingFailure>>> cir
    ) {
        CompletableFuture<Either<LevelChunk, ChunkHolder.ChunkLoadingFailure>> future = cir.getReturnValue();
        
        future.thenAcceptAsync((either) -> {
            either.mapLeft((worldChunk) -> {
                IPGlobal.chunkDataSyncManager.onChunkProvidedDeferred(worldChunk);
                
                return Either.left(worldChunk);
            });
        }, (runnable) -> {
            this.mainThreadMailbox.tell(ChunkTaskPriorityQueueSorter.message(chunkHolder, runnable));
        });
    }
}
