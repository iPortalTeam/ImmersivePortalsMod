package qouteall.imm_ptl.core.mixin.common.chunk_sync;

import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ChunkTaskPrioritySystem;
import net.minecraft.server.world.ServerLightingProvider;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.thread.MessageListener;
import net.minecraft.world.chunk.WorldChunk;
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

@Mixin(value = ThreadedAnvilChunkStorage.class, priority = 1100)
public abstract class MixinThreadedAnvilChunkStorage_C implements IEThreadedAnvilChunkStorage {
    @Shadow
    private int watchDistance;
    
    @Shadow
    @Final
    private ServerWorld world;
    
    @Shadow
    protected abstract ChunkHolder getChunkHolder(long long_1);
    
    @Shadow
    abstract void handlePlayerAddedOrRemoved(
        ServerPlayerEntity serverPlayerEntity_1,
        boolean boolean_1
    );
    
    @Shadow
    @Final
    private Int2ObjectMap entityTrackers;
    
    @Shadow
    @Final
    private AtomicInteger totalChunksLoadedCount;
    
    @Shadow
    @Final
    private MessageListener<ChunkTaskPrioritySystem.Task<Runnable>> mainExecutor;
    
    @Shadow
    @Nullable
    protected abstract NbtCompound getUpdatedChunkNbt(ChunkPos pos) throws IOException;
    
    @Shadow
    @Final
    private ServerLightingProvider lightingProvider;
    
    @Override
    public int getWatchDistance() {
        return watchDistance;
    }
    
    @Override
    public ServerWorld getWorld() {
        return world;
    }
    
    @Override
    public ServerLightingProvider getLightingProvider() {
        return lightingProvider;
    }
    
    @Override
    public ChunkHolder getChunkHolder_(long long_1) {
        return getChunkHolder(long_1);
    }
    
    @Override
    public boolean portal_isChunkGenerated(ChunkPos chunkPos) {
        if (world.getChunkManager().isChunkLoaded(chunkPos.x, chunkPos.z)) {
            return true;
        }
        
        try {
            NbtCompound tag = getUpdatedChunkNbt(chunkPos);
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
    private void sendChunkDataPackets(
        ServerPlayerEntity player, MutableObject<ChunkDataS2CPacket> cachedDataPacket, WorldChunk chunk
    ) {
        //chunk data packet will be sent on ChunkDataSyncManager
    }
    
    //do my packet sending
    @Inject(
        method = "makeChunkTickable",
        at = @At("RETURN"),
        cancellable = true
    )
    private void onCreateTickingFuture(
        ChunkHolder chunkHolder,
        CallbackInfoReturnable<CompletableFuture<Either<WorldChunk, ChunkHolder.Unloaded>>> cir
    ) {
        CompletableFuture<Either<WorldChunk, ChunkHolder.Unloaded>> future = cir.getReturnValue();
        
        future.thenAcceptAsync((either) -> {
            either.mapLeft((worldChunk) -> {
                IPGlobal.chunkDataSyncManager.onChunkProvidedDeferred(worldChunk);
                
                return Either.left(worldChunk);
            });
        }, (runnable) -> {
            this.mainExecutor.send(ChunkTaskPrioritySystem.createMessage(chunkHolder, runnable));
        });
    }
}
