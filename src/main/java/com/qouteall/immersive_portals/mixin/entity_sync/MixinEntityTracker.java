package com.qouteall.immersive_portals.mixin.entity_sync;

import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.MyNetwork;
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
public class MixinEntityTracker implements IEEntityTracker {
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
                entity.dimension,
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
                entity.dimension,
                packet_1
            )
        );
    }
    
    
    //copied
    private static int getChebyshevDistance(ChunkPos chunkPos_1, int int_1, int int_2) {
        int int_3 = chunkPos_1.x - int_1;
        int int_4 = chunkPos_1.z - int_2;
        return Math.max(Math.abs(int_3), Math.abs(int_4));
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
        IEThreadedAnvilChunkStorage storage = McHelper.getIEStorage(entity.dimension);
        
        if (player != this.entity) {
            if (entity.dimension != entity.world.dimension.getType()) {
                Helper.err(String.format(
                    "Entity dimension field abnormal. Force corrected. %s %s %s",
                    entity,
                    entity.dimension,
                    entity.world.dimension.getType()
                ));
                entity.dimension = entity.world.dimension.getType();
            }
    
            Vec3d relativePos = (player.getPos()).subtract(this.entry.getLastPos());
            int maxWatchDistance = Math.min(
                this.maxDistance,
                (storage.getWatchDistance() - 1) * 16
            );
            boolean isWatchedNow =
                NewChunkTrackingGraph.isPlayerWatchingChunkWithinRaidus(
                    player,
                    entity.dimension,
                    entity.chunkX,
                    entity.chunkZ,
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
    }
}
