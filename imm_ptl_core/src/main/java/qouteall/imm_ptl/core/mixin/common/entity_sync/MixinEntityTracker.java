package qouteall.imm_ptl.core.mixin.common.entity_sync;

import net.minecraft.entity.Entity;
import net.minecraft.network.Packet;
import net.minecraft.server.network.EntityTrackerEntry;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.EntityTrackingListener;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.profiler.Profiler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.chunk_loading.NewChunkTrackingGraph;
import qouteall.imm_ptl.core.ducks.IEEntityTracker;
import qouteall.imm_ptl.core.ducks.IEEntityTrackerEntry;
import qouteall.imm_ptl.core.ducks.IEThreadedAnvilChunkStorage;
import qouteall.imm_ptl.core.network.IPCommonNetwork;
import qouteall.imm_ptl.core.platform_specific.IPNetworking;

import java.util.List;
import java.util.Set;

//NOTE must redirect all packets about entities
@Mixin(ThreadedAnvilChunkStorage.EntityTracker.class)
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
    public abstract void stopTracking();
    
    @Shadow
    protected abstract int getMaxTrackDistance();
    
    @Shadow
    @Final
    private Set<EntityTrackingListener> listeners;
    
    @Shadow private ChunkSectionPos trackedSection;
    
    @Redirect(
        method = "Lnet/minecraft/server/world/ThreadedAnvilChunkStorage$EntityTracker;sendToOtherNearbyPlayers(Lnet/minecraft/network/Packet;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/world/EntityTrackingListener;sendPacket(Lnet/minecraft/network/Packet;)V"
        )
    )
    private void onSendToOtherNearbyPlayers(
        EntityTrackingListener entityTrackingListener, Packet<?> packet
    ) {
        IPCommonNetwork.withForceRedirect(
            ((ServerWorld) entity.world),
            () -> {
                entityTrackingListener.sendPacket(packet);
            }
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
        IPCommonNetwork.sendRedirectedPacket(serverPlayNetworkHandler, packet_1, entity.world.getRegistryKey());
    }
    
    /**
     * @author qouteall
     * @reason make incompat fail fast
     */
    @Overwrite
    public void updateTrackedStatus(ServerPlayerEntity player) {
        updateEntityTrackingStatus(player);
    }
    
    /**
     * @author qouteall
     * @reason make incompat fail fast
     */
    @Overwrite
    public void updateTrackedStatus(List<ServerPlayerEntity> list) {
        for (ServerPlayerEntity player : McHelper.getRawPlayerList()) {
            updateEntityTrackingStatus(player);
        }
    }
    
    @Override
    public Entity getEntity_() {
        return entity;
    }
    
    @Override
    public void updateEntityTrackingStatus(ServerPlayerEntity player) {
        IEThreadedAnvilChunkStorage storage = (IEThreadedAnvilChunkStorage)
            ((ServerWorld) entity.world).getChunkManager().threadedAnvilChunkStorage;
        
        if (player == this.entity) {
            return;
        }
        
        Profiler profiler = player.world.getProfiler();
        profiler.push("portal_entity_track");
        
        int maxWatchDistance = Math.min(
            this.getMaxTrackDistance(),
            (storage.getWatchDistance() - 1) * 16
        );
        ChunkPos chunkPos = entity.getChunkPos();
        boolean isWatchedNow =
            NewChunkTrackingGraph.isPlayerWatchingChunkWithinRaidus(
                player,
                this.entity.world.getRegistryKey(),
                chunkPos.x,
                chunkPos.z,
                maxWatchDistance
            ) && this.entity.canBeSpectated(player);
        if (isWatchedNow) {
            
            if (listeners.add(player.networkHandler)) {
                this.entry.startTracking(player);
            }
        }
        else if (listeners.remove(player.networkHandler)) {
            this.entry.stopTracking(player);
        }
        
        profiler.pop();
        
    }
    
    @Override
    public void onPlayerRespawn(ServerPlayerEntity oldPlayer) {
        listeners.remove(oldPlayer.networkHandler);
        entry.stopTracking(oldPlayer);
    }
    
    @Override
    public void resendSpawnPacketToTrackers() {
        // avoid sending wrong position delta update packet
        ((IEEntityTrackerEntry) entry).ip_updateTrackedEntityPosition();
        
        Packet<?> spawnPacket = entity.createSpawnPacket();
        Packet redirected = IPNetworking.createRedirectedMessage(entity.world.getRegistryKey(), spawnPacket);
        listeners.forEach(handler -> {
            handler.sendPacket(redirected);
        });
    }
    
    @Override
    public void stopTrackingToAllPlayers_() {
        stopTracking();
    }
    
    @Override
    public void tickEntry() {
        entry.tick();
    }
    
    @Override
    public ChunkSectionPos getLastCameraPosition() {
        return trackedSection;
    }
    
    @Override
    public void setLastCameraPosition(ChunkSectionPos arg) {
        trackedSection = arg;
    }
}
