package com.qouteall.immersive_portals.mixin;

import com.qouteall.immersive_portals.Globals;
import com.qouteall.immersive_portals.chunk_loading.ChunkDataSyncManager;
import com.qouteall.immersive_portals.chunk_loading.DimensionalChunkPos;
import com.qouteall.immersive_portals.chunk_loading.RedirectedMessageManager;
import net.minecraft.client.network.packet.EntitiesDestroyS2CPacket;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.Packet;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayDeque;
import java.util.stream.Collectors;

@Mixin(ServerPlayerEntity.class)
public class MixinServerPlayerEntity {
    @Shadow
    public ServerPlayNetworkHandler networkHandler;
    ArrayDeque<Entity> myRemovedEntities;
    
    @Inject(
        method = "Lnet/minecraft/server/network/ServerPlayerEntity;sendInitialChunkPackets(Lnet/minecraft/util/math/ChunkPos;Lnet/minecraft/network/Packet;Lnet/minecraft/network/Packet;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onSendChunkDataPackets(
        ChunkPos chunkPos_1,
        Packet<?> packet_1,
        Packet<?> packet_2,
        CallbackInfo ci
    ) {
        ServerPlayerEntity this_ = (ServerPlayerEntity) (Object) this;
        DimensionalChunkPos chunkPos = new DimensionalChunkPos(
            this_.dimension, chunkPos_1
        );
        boolean isWatching = Globals.chunkTracker.isPlayerWatchingChunk(
            this_, chunkPos
        );
        boolean isManagedByVanilla = ChunkDataSyncManager.isChunkManagedByVanilla(
            (ServerPlayerEntity) (Object) this, chunkPos
        );
        if (!isManagedByVanilla && isWatching) {
            ci.cancel();
        }
    }
    
    @Inject(
        method = "Lnet/minecraft/server/network/ServerPlayerEntity;sendUnloadChunkPacket(Lnet/minecraft/util/math/ChunkPos;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onSendUnloadChunkPacket(ChunkPos chunkPos_1, CallbackInfo ci) {
        ServerPlayerEntity this_ = (ServerPlayerEntity) (Object) this;
        DimensionalChunkPos dimensionalChunkPos = new DimensionalChunkPos(
            this_.dimension,
            chunkPos_1
        );
        
        Globals.chunkDataSyncManager.manageToSendUnloadPacket(
            this_, dimensionalChunkPos
        );
        
        ci.cancel();
    }
    
    @Inject(
        method = "Lnet/minecraft/server/network/ServerPlayerEntity;tick()V",
        at = @At("TAIL")
    )
    private void onTicking(CallbackInfo ci) {
        if (myRemovedEntities != null) {
            myRemovedEntities.stream()
                .collect(Collectors.groupingBy(entity -> entity.dimension))
                .forEach((dimension, list) -> networkHandler.sendPacket(
                    RedirectedMessageManager.createRedirectedMessage(
                        dimension,
                        new EntitiesDestroyS2CPacket(
                            list.stream().mapToInt(
                                Entity::getEntityId
                            ).toArray()
                        )
                    )
                ));
            myRemovedEntities = null;
        }
    }
    
    /**
     * @author qouteall
     */
    @Overwrite
    public void onStoppedTracking(Entity entity_1) {
        if (entity_1 instanceof PlayerEntity) {
            this.networkHandler.sendPacket(
                RedirectedMessageManager.createRedirectedMessage(
                    entity_1.dimension,
                    new EntitiesDestroyS2CPacket(entity_1.getEntityId())
                )
            );
        }
        else {
            if (myRemovedEntities == null) {
                myRemovedEntities = new ArrayDeque<>();
            }
            myRemovedEntities.add(entity_1);
        }
        
    }
    
    /**
     * @author qouteall
     */
    @Overwrite
    public void onStartedTracking(Entity entity_1) {
        if (myRemovedEntities != null) {
            myRemovedEntities.remove(entity_1);
        }
    }
}
