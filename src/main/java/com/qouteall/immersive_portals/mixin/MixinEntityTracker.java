package com.qouteall.immersive_portals.mixin;

import com.google.common.collect.Sets;
import com.qouteall.immersive_portals.Globals;
import com.qouteall.immersive_portals.chunk_loading.DimensionalChunkPos;
import com.qouteall.immersive_portals.chunk_loading.RedirectedMessageManager;
import com.qouteall.immersive_portals.exposer.IEThreadedAnvilChunkStorage;
import com.qouteall.immersive_portals.my_util.Helper;
import net.minecraft.entity.Entity;
import net.minecraft.network.Packet;
import net.minecraft.server.network.EntityTrackerEntry;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

//NOTE must redirect all packets about entities
@Mixin(targets = "net.minecraft.server.world.ThreadedAnvilChunkStorage$EntityTracker")
public class MixinEntityTracker {
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
    private Set<ServerPlayerEntity> playersTracking = Sets.newHashSet();
    
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
            RedirectedMessageManager.createRedirectedMessage(
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
            RedirectedMessageManager.createRedirectedMessage(
                entity.dimension,
                packet_1
            )
        );
    }
    
    //copied
    private static int getChebyshevDistance(
        ChunkPos chunkPos_1,
        ServerPlayerEntity serverPlayerEntity_1,
        boolean boolean_1
    ) {
        int int_3;
        int int_4;
        if (boolean_1) {
            ChunkSectionPos chunkSectionPos_1 = serverPlayerEntity_1.getCameraPosition();
            int_3 = chunkSectionPos_1.getChunkX();
            int_4 = chunkSectionPos_1.getChunkZ();
        }
        else {
            int_3 = MathHelper.floor(serverPlayerEntity_1.x / 16.0D);
            int_4 = MathHelper.floor(serverPlayerEntity_1.z / 16.0D);
        }
        
        return getChebyshevDistance(chunkPos_1, int_3, int_4);
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
        IEThreadedAnvilChunkStorage storage = Helper.getIEStorage(entity.dimension);
        
        if (player != this.entity) {
            Vec3d relativePos = (new Vec3d(
                player.x,
                player.y,
                player.z
            )).subtract(this.entry.method_18759());
            int maxWatchDistance = Math.min(
                this.maxDistance,
                (storage.getWatchDistance() - 1) * 16
            );
            boolean isWatchedNow =
                player.dimension == entity.dimension &&
                    relativePos.x >= (double) (-maxWatchDistance) &&
                    relativePos.x <= (double) maxWatchDistance &&
                    relativePos.z >= (double) (-maxWatchDistance) &&
                    relativePos.z <= (double) maxWatchDistance &&
                    this.entity.canBeSpectated(player);
            isWatchedNow = isWatchedNow ||
                Globals.chunkTracker.isPlayerWatchingChunk(
                    player,
                    new DimensionalChunkPos(
                        entity.dimension,
                        new ChunkPos(entity.getBlockPos())
                    )
                );
            if (isWatchedNow) {
                boolean boolean_2 = this.entity.teleporting;
                if (!boolean_2) {
                    ChunkPos chunkPos_1 = new ChunkPos(this.entity.chunkX, this.entity.chunkZ);
                    ChunkHolder chunkHolder_1 = storage.getChunkHolder_(chunkPos_1.toLong());
                    if (chunkHolder_1 != null && chunkHolder_1.getWorldChunk() != null) {
                        boolean_2 = getChebyshevDistance(
                            chunkPos_1,
                            player,
                            false
                        ) <= storage.getWatchDistance();
                    }
                }
                
                if (boolean_2 && this.playersTracking.add(player)) {
                    this.entry.startTracking(player);
                }
            }
            else if (this.playersTracking.remove(player)) {
                this.entry.stopTracking(player);
            }
            
        }
    }
    
    /**
     * @author qouteall
     * performance may be slowed down
     */
    @Overwrite
    public void updateCameraPosition(List<ServerPlayerEntity> list_1) {
        ArrayList<ServerPlayerEntity> playerList =
            new ArrayList<>(Helper.getServer().getPlayerManager().getPlayerList());
        
        playerList.forEach(this::updateCameraPosition);
        
    }
}
