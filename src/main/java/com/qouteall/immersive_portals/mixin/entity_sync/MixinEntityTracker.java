package com.qouteall.immersive_portals.mixin.entity_sync;

import com.qouteall.hiding_in_the_bushes.MyNetwork;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.chunk_loading.NewChunkTrackingGraph;
import com.qouteall.immersive_portals.ducks.IEEntityTracker;
import com.qouteall.immersive_portals.ducks.IEThreadedAnvilChunkStorage;
import net.minecraft.entity.Entity;
import net.minecraft.network.Packet;
import net.minecraft.server.network.EntityTrackerEntry;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import java.util.Set;

//NOTE must redirect all packets about entities
@Mixin(targets = "net.minecraft.server.world.ThreadedAnvilChunkStorage$EntityTracker")
public abstract class MixinEntityTracker implements IEEntityTracker {
    @Shadow
    @Final
    private EntityTrackerEntry entry;
    @Shadow
    @Final
    private Entity entity;
    @Shadow
    @Final
    private int maxDistance;
    @Shadow
    private ChunkSectionPos lastCameraPosition;
    @Shadow
    @Final
    private Set<ServerPlayerEntity> playersTracking;
    
    @Shadow
    public abstract void stopTracking();
    
    @Shadow protected abstract int getMaxTrackDistance();
    
    @Redirect(
        method = "Lnet/minecraft/server/world/ThreadedAnvilChunkStorage$EntityTracker;sendToOtherNearbyPlayers(Lnet/minecraft/network/Packet;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerPlayNetworkHandler;sendPacket(Lnet/minecraft/network/Packet;)V"
        )
    )
    private void onSendToOtherNearbyPlayers(
        ServerPlayNetworkHandler serverPlayNetworkHandler,
        Packet<?> packet_1
    ) {
        serverPlayNetworkHandler.sendPacket(
            MyNetwork.createRedirectedMessage(
                entity.world.getRegistryKey(),
                packet_1
            )
        );
    }
    
    @Redirect(
        method = "Lnet/minecraft/server/world/ThreadedAnvilChunkStorage$EntityTracker;sendToNearbyPlayers(Lnet/minecraft/network/Packet;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerPlayNetworkHandler;sendPacket(Lnet/minecraft/network/Packet;)V"
        )
    )
    private void onSendToNearbyPlayers(
        ServerPlayNetworkHandler serverPlayNetworkHandler,
        Packet<?> packet_1
    ) {
        serverPlayNetworkHandler.sendPacket(
            MyNetwork.createRedirectedMessage(
                entity.world.getRegistryKey(),
                packet_1
            )
        );
    }
    
    /**
     * @author qouteall
     */
    @Overwrite
    public void updateCameraPosition(ServerPlayerEntity player) {
        updateCameraPosition_(player);
    }
    
    /**
     * @author qouteall
     * performance may be slowed down
     */
    @Overwrite
    public void updateCameraPosition(List<ServerPlayerEntity> list_1) {
        //ignore the argument
        
        McHelper.getRawPlayerList().forEach(this::updateCameraPosition);
        
    }
    
    @Override
    public Entity getEntity_() {
        return entity;
    }
    
    @Override
    public void updateCameraPosition_(ServerPlayerEntity player) {
        IEThreadedAnvilChunkStorage storage = McHelper.getIEStorage(entity.world.getRegistryKey());
        
        if (player != this.entity) {
            McHelper.checkDimension(this.entity);
            
            Vec3d relativePos = (player.getPos()).subtract(this.entry.getLastPos());
            int maxWatchDistance = Math.min(
                this.getMaxTrackDistance(),
                (storage.getWatchDistance() - 1) * 16
            );
            boolean isWatchedNow =
                NewChunkTrackingGraph.isPlayerWatchingChunkWithinRaidus(
                    player,
                    this.entity.world.getRegistryKey(),
                    this.entity.chunkX,
                    this.entity.chunkZ,
                    maxWatchDistance
                ) &&
                    this.entity.canBeSpectated(player);
            if (isWatchedNow) {
                boolean shouldTrack = this.entity.teleporting;
                if (!shouldTrack) {
                    ChunkPos chunkPos_1 = new ChunkPos(this.entity.chunkX, this.entity.chunkZ);
                    ChunkHolder chunkHolder_1 = storage.getChunkHolder_(chunkPos_1.toLong());
                    if (chunkHolder_1 != null && chunkHolder_1.getWorldChunk() != null) {
                        shouldTrack = true;
                    }
                }
                
                if (shouldTrack && this.playersTracking.add(player)) {
                    this.entry.startTracking(player);
                }
            }
            else if (this.playersTracking.remove(player)) {
                this.entry.stopTracking(player);
            }
            
        }
    }
    
    @Override
    public void onPlayerRespawn(ServerPlayerEntity oldPlayer) {
        playersTracking.remove(oldPlayer);
        entry.stopTracking(oldPlayer);
    }
    
    @Override
    public void resendSpawnPacketToTrackers() {
        Packet<?> spawnPacket = entity.createSpawnPacket();
        Packet redirected = MyNetwork.createRedirectedMessage(entity.world.getRegistryKey(), spawnPacket);
        playersTracking.forEach(player -> {
            player.networkHandler.sendPacket(redirected);
        });
    }
    
    @Override
    public void stopTrackingToAllPlayers_() {
        stopTracking();
    }
}
