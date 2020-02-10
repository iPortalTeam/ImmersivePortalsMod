package com.qouteall.immersive_portals.mixin.chunk_sync;

import com.mojang.datafixers.util.Either;
import com.qouteall.immersive_portals.SGlobal;
import com.qouteall.immersive_portals.ducks.IEEntityTracker;
import com.qouteall.immersive_portals.ducks.IEThreadedAnvilChunkStorage;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.entity.Entity;
import net.minecraft.network.Packet;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.*;
import net.minecraft.util.thread.MessageListener;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@Mixin(ThreadedAnvilChunkStorage.class)
public abstract class MixinThreadedAnvilChunkStorage implements IEThreadedAnvilChunkStorage {
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
    
    /**
     * @author qouteall
     */
    @Overwrite
    private void sendChunkDataPackets(
        ServerPlayerEntity player,
        Packet<?>[] packets_1,
        WorldChunk worldChunk_1
    ) {
        //chunk data packet will be sent on ChunkDataSyncManager
    }
    
    @Inject(
        method = "unloadEntity",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onUnloadEntity(Entity entity, CallbackInfo ci) {
        //when the player leave this dimension, do not stop tracking entities
        if (entity instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) entity;
            if (SGlobal.serverTeleportationManager.isTeleporting(player)) {
                entityTrackers.remove(entity.getEntityId());
                handlePlayerAddedOrRemoved(player, false);
                ci.cancel();
            }
        }
    }
    
    //cancel vanilla packet sending
    @Redirect(
        method = "createTickingFuture",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/concurrent/CompletableFuture;thenAcceptAsync(Ljava/util/function/Consumer;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;"
        )
    )
    private CompletableFuture<Void> redirectThenAcceptAsync(
        CompletableFuture completableFuture,
        Consumer<?> action,
        Executor executor
    ) {
        return null;
    }
    
    //do my packet sending
    @Inject(
        method = "createTickingFuture",
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
                
                SGlobal.chunkDataSyncManager.onChunkProvidedDeferred(worldChunk);
                
                return Either.left(worldChunk);
            });
        }, (runnable) -> {
            this.mainExecutor.send(ChunkTaskPrioritySystem.createMessage(chunkHolder, runnable));
        });
    }
    
    @Override
    public void onPlayerRespawn(ServerPlayerEntity oldPlayer) {
        entityTrackers.values().forEach(obj -> {
            ((IEEntityTracker) obj).onPlayerRespawn(oldPlayer);
        });
    }
    
}
