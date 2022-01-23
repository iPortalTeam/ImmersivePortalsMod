package qouteall.imm_ptl.core.mixin.common;

import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.chunk_loading.NewChunkTrackingGraph;
import qouteall.imm_ptl.core.platform_specific.IPNetworking;
import qouteall.imm_ptl.core.portal.global_portals.GlobalPortalStorage;

import javax.annotation.Nullable;
import java.util.List;

@Mixin(PlayerList.class)
public class MixinPlayerManager {
    @Shadow
    @Final
    private List<ServerPlayer> players;
    
    @Shadow
    @Final
    private MinecraftServer server;
    
    @Inject(
        method = "Lnet/minecraft/server/players/PlayerList;placeNewPlayer(Lnet/minecraft/network/Connection;Lnet/minecraft/server/level/ServerPlayer;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/protocol/game/ClientboundLoginPacket;<init>(IZLnet/minecraft/world/level/GameType;Lnet/minecraft/world/level/GameType;Ljava/util/Set;Lnet/minecraft/core/RegistryAccess$RegistryHolder;Lnet/minecraft/world/level/dimension/DimensionType;Lnet/minecraft/resources/ResourceKey;JIIIZZZZ)V"
        )
    )
    private void onConnectionEstablished(
        Connection connection,
        ServerPlayer player,
        CallbackInfo ci
    ) {
        player.connection.send(IPNetworking.createDimSync());
    }
    
    @Inject(method = "Lnet/minecraft/server/players/PlayerList;sendLevelInfo(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/server/level/ServerLevel;)V", at = @At("RETURN"))
    private void onSendWorldInfo(ServerPlayer player, ServerLevel world, CallbackInfo ci) {
        if (!IPGlobal.serverTeleportationManager.isFiringMyChangeDimensionEvent) {
            GlobalPortalStorage.onPlayerLoggedIn(player);
        }
    }
    
    @Inject(method = "Lnet/minecraft/server/players/PlayerList;placeNewPlayer(Lnet/minecraft/network/Connection;Lnet/minecraft/server/level/ServerPlayer;)V", at = @At("TAIL"))
    private void onOnPlayerConnect(Connection connection, ServerPlayer player, CallbackInfo ci) {
        NewChunkTrackingGraph.updateForPlayer(player);
    }
    
    //with redirection
    @Inject(
        method = "Lnet/minecraft/server/players/PlayerList;broadcastAll(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/resources/ResourceKey;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    public void sendToDimension(Packet<?> packet, ResourceKey<Level> dimension, CallbackInfo ci) {
        for (ServerPlayer player : players) {
            if (player.level.dimension() == dimension) {
                player.connection.send(
                    IPNetworking.createRedirectedMessage(
                        dimension,
                        packet
                    )
                );
            }
        }
        
        ci.cancel();
    }
    
    /**
     * @author qoutall
     * mostly for sound events
     * @reason make incompat fail fast
     */
    @Overwrite
    public void broadcast(
        @Nullable Player excludingPlayer,
        double x, double y, double z, double distance,
        ResourceKey<Level> dimension, Packet<?> packet
    ) {
        ChunkPos chunkPos = new ChunkPos(new BlockPos(new Vec3(x, y, z)));
        
        NewChunkTrackingGraph.getPlayersViewingChunk(
            dimension, chunkPos.x, chunkPos.z
        ).filter(playerEntity -> NewChunkTrackingGraph.isPlayerWatchingChunkWithinRaidus(
            playerEntity, dimension, chunkPos.x, chunkPos.z, (int) distance + 16
        )).forEach(playerEntity -> {
            if (playerEntity != excludingPlayer) {
                playerEntity.connection.send(IPNetworking.createRedirectedMessage(
                    dimension, packet
                ));
            }
        });
    }
}
