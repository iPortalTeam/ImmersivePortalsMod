package qouteall.imm_ptl.core.mixin.common.entity_sync;

import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
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
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.chunk_loading.ImmPtlChunkTracking;
import qouteall.imm_ptl.core.ducks.IEChunkMap;
import qouteall.imm_ptl.core.ducks.IEEntityTrackerEntry;
import qouteall.imm_ptl.core.ducks.IETrackedEntity;
import qouteall.imm_ptl.core.miscellaneous.IPVanillaCopy;
import qouteall.imm_ptl.core.network.PacketRedirection;

import java.util.List;
import java.util.Set;

//NOTE must redirect all packets about entities
@Mixin(ChunkMap.TrackedEntity.class)
public abstract class MixinTrackedEntity implements IETrackedEntity {
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
    
    @Shadow
    private SectionPos lastSectionPos;
    
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
        PacketRedirection.withForceRedirect(
            ((ServerLevel) entity.level()),
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
        Packet packet_1
    ) {
        PacketRedirection.sendRedirectedPacket(serverPlayNetworkHandler, packet_1, entity.level().dimension());
    }
    
    // Note VMP redirects getEffectiveRange()
    @Inject(method = "updatePlayer", at = @At("HEAD"), cancellable = true)
    private void onUpdatePlayer(ServerPlayer player, CallbackInfo ci) {
        ip_updateEntityTrackingStatus(player);
        ci.cancel();
    }
    
    /**
     * @author qouteall
     * @reason make incompat fail fast
     */
    @Overwrite
    public void updatePlayers(List<ServerPlayer> list) {
        for (ServerPlayer player : McHelper.getRawPlayerList()) {
            ip_updateEntityTrackingStatus(player);
        }
    }
    
    @Override
    public Entity ip_getEntity() {
        return entity;
    }
    
    /**
     * {@link ChunkMap.TrackedEntity#updatePlayer(ServerPlayer)}
     */
    @IPVanillaCopy
    @Override
    public void ip_updateEntityTrackingStatus(ServerPlayer player) {
        IEChunkMap storage = (IEChunkMap)
            ((ServerLevel) entity.level()).getChunkSource().chunkMap;
        
        if (player == this.entity) {
            return;
        }
        
        ProfilerFiller profiler = player.level().getProfiler();
        profiler.push("portal_entity_track");
        
        int maxWatchDistance = Math.min(
            this.getEffectiveRange(),
            (storage.ip_getWatchDistance() - 1) * 16
        );
        ChunkPos chunkPos = entity.chunkPosition();
        boolean isWatchedNow =
            ImmPtlChunkTracking.isPlayerWatchingChunkWithinRadius(
                player,
                this.entity.level().dimension(),
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
    public void ip_onDimensionRemove() {
        for (ServerPlayerConnection connection : seenBy) {
            serverEntity.removePairing(connection.getPlayer());
        }
        seenBy.clear();
    }
    
    @Override
    public void ip_resendSpawnPacketToTrackers() {
        // avoid sending wrong position delta update packet
        ((IEEntityTrackerEntry) serverEntity).ip_updateTrackedEntityPosition();
        
        Packet spawnPacket = entity.getAddEntityPacket();
        Packet<ClientGamePacketListener> redirected = PacketRedirection.createRedirectedMessage(
            entity.getServer(),
            entity.level().dimension(), spawnPacket
        );
        seenBy.forEach(handler -> {
            handler.send(redirected);
        });
    }
    
    @Override
    public void ip_stopTrackingToAllPlayers() {
        broadcastRemoved();
    }
    
    @Override
    public void ip_tickEntry() {
        serverEntity.sendChanges();
    }
    
    @Override
    public SectionPos ip_getLastCameraPosition() {
        return lastSectionPos;
    }
    
    @Override
    public void ip_setLastCameraPosition(SectionPos arg) {
        lastSectionPos = arg;
    }
    
}
