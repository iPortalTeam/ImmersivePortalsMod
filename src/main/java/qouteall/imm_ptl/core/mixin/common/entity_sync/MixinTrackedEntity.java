package qouteall.imm_ptl.core.mixin.common.entity_sync;

import net.minecraft.core.SectionPos;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.chunk_loading.ImmPtlChunkTracking;
import qouteall.imm_ptl.core.ducks.IEChunkMap;
import qouteall.imm_ptl.core.ducks.IEEntityTrackerEntry;
import qouteall.imm_ptl.core.ducks.IETrackedEntity;
import qouteall.imm_ptl.core.miscellaneous.IPVanillaCopy;
import qouteall.imm_ptl.core.mixin.common.other_sync.MixinPlayerList;
import qouteall.imm_ptl.core.network.PacketRedirection;

import java.util.List;
import java.util.Map;
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
        Packet packet
    ) {
        PacketRedirection.sendRedirectedPacket(
            serverPlayNetworkHandler, packet, entity.level().dimension()
        );
    }
    
    /**
     * @author qouteall
     * @reason managed by ImmPtl
     * In vanilla, entity tracking updates when
     * - {@link ChunkMap#move(ServerPlayer)}
     *   When the player moves, the entities in curr dim except that player updates to that player,
     *   and that player updates to all player in that dimension.
     *   In ImmPtl, handled in {@link ImmPtlChunkTracking#tick(MinecraftServer)}
     *
     * - {@link ChunkMap#addEntity(Entity)}
     *   When adding a new entity, it's updated to all players in curr dim.
     *   When adding a new player, all entities (except the player) in curr dim updates to that player.
     *   In ImmPtl, adding entity is handled in
     *   {@link MixinChunkMap_E#redirectUpdatePlayers(ChunkMap.TrackedEntity, List)}
     *   player login is handled in
     *   {@link MixinPlayerList#onOnPlayerConnect(Connection, ServerPlayer, CommonListenerCookie, CallbackInfo)}
     *
     * - {@link ChunkMap#tick()}
     *   If a player's section pos changes, all entities in curr dim updates to that player.
     *   If an entity's section pos changes, it's updates to all players in that dimension.
     *   In ImmPtl, handled in {@link ImmPtlChunkTracking#tick(MinecraftServer)}
     */
    @Overwrite
    public void updatePlayer(ServerPlayer player) {
        // nothing
    }
    
    /**
     * @author qouteall
     * @reason managed by ImmPtl
     */
    @Overwrite
    public void updatePlayers(List<ServerPlayer> list) {
        // nothing
    }
    
    @Override
    public Entity ip_getEntity() {
        return entity;
    }
    
    /**
     * {@link ChunkMap.TrackedEntity#updatePlayer(ServerPlayer)}
     * This only checks the players viewing the chunk.
     * Vanilla checks all players in dimension {@link ChunkMap#tick()}
     * so this is more efficient in this aspect.
     * However, in vanilla, if both the entity and player doesn't move, it won't update.
     * But in ImmPtl, it constantly updates because portals can change at any time and
     * that can changes entity visibility at anytime.
     */
    @IPVanillaCopy
    @Override
    public void ip_updateEntityTrackingStatus() {
        IEChunkMap chunkMap = (IEChunkMap)
            ((ServerLevel) entity.level()).getChunkSource().chunkMap;
        
        var watchRecMap = ImmPtlChunkTracking.getWatchRecordForChunk(
            entity.level().dimension(),
            entity.chunkPosition().x, entity.chunkPosition().z
        );
        
        // no need to clamp it with render distance, as we check chunk watch records now
        int effectiveRange = getEffectiveRange();
        
        seenBy.removeIf(connection -> {
            ServerPlayer player = connection.getPlayer();
            boolean shouldRemove = !watches(entity, watchRecMap, effectiveRange, player);
            if (shouldRemove) {
                PacketRedirection.withForceRedirect(
                    ((ServerLevel) entity.level()),
                    () -> {
                        this.serverEntity.removePairing(player);
                    }
                );
            }
            return shouldRemove;
        });
        
        if (watchRecMap != null) {
            for (var e : watchRecMap.entrySet()) {
                ServerPlayer player = e.getKey();
                ImmPtlChunkTracking.PlayerWatchRecord rec = e.getValue();
                
                if (recWatches(entity, effectiveRange, rec, player)) {
                    if (seenBy.add(player.connection)) {
                        PacketRedirection.withForceRedirect(
                            ((ServerLevel) entity.level()),
                            () -> {
                                this.serverEntity.addPairing(player);
                            }
                        );
                    }
                }
            }
        }
    }
    
    @Unique
    private static boolean watches(
        Entity entity,
        @Nullable Map<ServerPlayer, ImmPtlChunkTracking.PlayerWatchRecord> watchRec,
        int effectiveRange,
        ServerPlayer player
    ) {
        if (watchRec == null) {
            return false;
        }
        
        if (entity == player) {
            return false;
        }
        
        ImmPtlChunkTracking.PlayerWatchRecord rec = watchRec.get(player);
        
        return recWatches(entity, effectiveRange, rec, player);
    }
    
    @Unique
    private static boolean recWatches(
        Entity entity, int effectiveRange,
        ImmPtlChunkTracking.PlayerWatchRecord rec, ServerPlayer player
    ) {
        if (rec == null) {
            return false;
        }
        
        if (!rec.isLoadedToPlayer) {
            // when player logging in standing on cross portal collision
            // we need to send add entity packet early,
            // otherwise cross portal collision will not work early enough
            // but if the portal is big (center can be 4 chunks away from player)
            // the new slow chunk sending mechanism will slow its packet sent
            // TODO find a solution to this
            return false;
        }
        
        if (entity == player) {
            return false;
        }
        
        return rec.distanceToSource * 16 + 8 <= effectiveRange;
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
    public void ip_sendChanges() {
        serverEntity.sendChanges();
    }
    
    @Override
    public SectionPos ip_getLastSectionPos() {
        return lastSectionPos;
    }
    
    @Override
    public void ip_setLastSectionPos(SectionPos arg) {
        lastSectionPos = arg;
    }
    
}
