package com.qouteall.immersive_portals.mixin.common.chunk_sync;

import com.mojang.datafixers.util.Either;
import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.ducks.IEThreadedAnvilChunkStorage;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.Packet;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ChunkTaskPrioritySystem;
import net.minecraft.server.world.ServerLightingProvider;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.thread.MessageListener;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

@Mixin(value = ThreadedAnvilChunkStorage.class, priority = 1100)
public abstract class MixinThreadedAnvilChunkStorage_C implements IEThreadedAnvilChunkStorage {
    @Shadow
    private int watchDistance;
    
    @Shadow
    @Final
    private ServerLightingProvider serverLightingProvider;
    
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
    @Final
    private File saveDir;
    
    @Shadow
    protected abstract NbtCompound getUpdatedChunkTag(ChunkPos pos) throws IOException;
    
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
        return serverLightingProvider;
    }
    
    @Override
    public ChunkHolder getChunkHolder_(long long_1) {
        return getChunkHolder(long_1);
    }
    
    @Override
    public File portal_getSaveDir() {
        return saveDir;
    }
    
    @Override
    public boolean portal_isChunkGenerated(ChunkPos chunkPos) {
        if (world.getChunkManager().isChunkLoaded(chunkPos.x, chunkPos.z)) {
            return true;
        }
        
        try {
            NbtCompound tag = getUpdatedChunkTag(chunkPos);
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
    public void sendChunkDataPackets(
        ServerPlayerEntity player,
        Packet<?>[] packets_1,
        WorldChunk worldChunk_1
    ) {
        //chunk data packet will be sent on ChunkDataSyncManager
    }
    
    //cancel vanilla packet sending
//    @Redirect(
//        method = "makeChunkTickable",
//        at = @At(
//            value = "INVOKE",
//            target = "Ljava/util/concurrent/CompletableFuture;thenAcceptAsync(Ljava/util/function/Consumer;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;"
//        )
//    )
//    private CompletableFuture<Void> redirectThenAcceptAsync(
//        CompletableFuture completableFuture,
//        Consumer<?> action,
//        Executor executor
//    ) {
//        return null;
//    }
    
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
                this.totalChunksLoadedCount.getAndIncrement();
                
                Global.chunkDataSyncManager.onChunkProvidedDeferred(worldChunk);
                
                return Either.left(worldChunk);
            });
        }, (runnable) -> {
            this.mainExecutor.send(ChunkTaskPrioritySystem.createMessage(chunkHolder, runnable));
        });
    }
}
