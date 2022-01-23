package qouteall.imm_ptl.core.mixin.common.entity_sync;

import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
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
@Mixin(ChunkMap.TrackedEntity.class)
public abstract class MixinEntityTracker implements IEEntityTracker {
    @Shadow
    @Final
    private ServerEntity serverEntity;
    @Shadow
    @Final
    private Entity entity;
    @Shadow
    @Final
    private int range;
    
    @Shadow
    public abstract void broadcastRemoved();
    
    @Shadow
    protected abstract int getEffectiveRange();
    
    @Shadow
    @Final
    private Set<ServerPlayerConnection> seenBy;
    
    @Shadow private SectionPos lastSectionPos;
    
    @Redirect(
        method = "Lnet/minecraft/server/level/ChunkMap$TrackedEntity;broadcast(Lnet/minecraft/network/protocol/Packet;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerPlayerConnection;send(Lnet/minecraft/network/protocol/Packet;)V"
        )
    )
    private void onSendToOtherNearbyPlayers(
        ServerPlayerConnection entityTrackingListener, Packet<?> packet
    ) {
        IPCommonNetwork.withForceRedirect(
            ((ServerLevel) entity.level),
            () -> {
                entityTrackingListener.send(packet);
            }
        );
    }
    
    @Redirect(
        method = "Lnet/minecraft/server/level/ChunkMap$TrackedEntity;broadcastAndSend(Lnet/minecraft/network/protocol/Packet;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;send(Lnet/minecraft/network/protocol/Packet;)V"
        )
    )
    private void onSendToNearbyPlayers(
        ServerGamePacketListenerImpl serverPlayNetworkHandler,
        Packet<?> packet_1
    ) {
        IPCommonNetwork.sendRedirectedPacket(serverPlayNetworkHandler, packet_1, entity.level.dimension());
    }
    
    /**
     * @author qouteall
     * @reason make incompat fail fast
     */
    @Overwrite
    public void updatePlayer(ServerPlayer player) {
        updateEntityTrackingStatus(player);
    }
    
    /**
     * @author qouteall
     * @reason make incompat fail fast
     */
    @Overwrite
    public void updatePlayers(List<ServerPlayer> list) {
        for (ServerPlayer player : McHelper.getRawPlayerList()) {
            updateEntityTrackingStatus(player);
        }
    }
    
    @Override
    public Entity getEntity_() {
        return entity;
    }
    
    @Override
    public void updateEntityTrackingStatus(ServerPlayer player) {
        IEThreadedAnvilChunkStorage storage = (IEThreadedAnvilChunkStorage)
            ((ServerLevel) entity.level).getChunkSource().chunkMap;
        
        if (player == this.entity) {
            return;
        }
        
        ProfilerFiller profiler = player.level.getProfiler();
        profiler.push("portal_entity_track");
        
        int maxWatchDistance = Math.min(
            this.getEffectiveRange(),
            (storage.getWatchDistance() - 1) * 16
        );
        ChunkPos chunkPos = entity.chunkPosition();
        boolean isWatchedNow =
            NewChunkTrackingGraph.isPlayerWatchingChunkWithinRaidus(
                player,
                this.entity.level.dimension(),
                chunkPos.x,
                chunkPos.z,
                maxWatchDistance
            ) && this.entity.broadcastToPlayer(player);
        if (isWatchedNow) {
            
            if (seenBy.add(player.connection)) {
                this.serverEntity.addPairing(player);
            }
        }
        else if (seenBy.remove(player.connection)) {
            this.serverEntity.removePairing(player);
        }
        
        profiler.pop();
        
    }
    
    @Override
    public void onPlayerRespawn(ServerPlayer oldPlayer) {
        seenBy.remove(oldPlayer.connection);
        serverEntity.removePairing(oldPlayer);
    }
    
    @Override
    public void resendSpawnPacketToTrackers() {
        // avoid sending wrong position delta update packet
        ((IEEntityTrackerEntry) serverEntity).ip_updateTrackedEntityPosition();
        
        Packet<?> spawnPacket = entity.getAddEntityPacket();
        Packet redirected = IPNetworking.createRedirectedMessage(entity.level.dimension(), spawnPacket);
        seenBy.forEach(handler -> {
            handler.send(redirected);
        });
    }
    
    @Override
    public void stopTrackingToAllPlayers_() {
        broadcastRemoved();
    }
    
    @Override
    public void tickEntry() {
        serverEntity.sendChanges();
    }
    
    @Override
    public SectionPos getLastCameraPosition() {
        return lastSectionPos;
    }
    
    @Override
    public void setLastCameraPosition(SectionPos arg) {
        lastSectionPos = arg;
    }
}
