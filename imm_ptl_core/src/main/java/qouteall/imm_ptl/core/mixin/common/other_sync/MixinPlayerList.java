package qouteall.imm_ptl.core.mixin.common.other_sync;

import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.chunk_loading.NewChunkTrackingGraph;
import qouteall.imm_ptl.core.network.PacketRedirection;
import qouteall.imm_ptl.core.portal.global_portals.GlobalPortalStorage;

import java.util.List;
import java.util.Set;

@Mixin(value = PlayerList.class, priority = 800)
public class MixinPlayerList {
    @Shadow
    @Final
    private List<ServerPlayer> players;
    
    @Shadow
    @Final
    private MinecraftServer server;
    
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
            if (player.level().dimension() == dimension) {
                PacketRedirection.sendRedirectedMessage(
                    player,
                    dimension,
                    packet
                );
            }
        }
        
        ci.cancel();
    }
    
    /**
     * correct the player reference, so that in
     * {@link qouteall.imm_ptl.core.mixin.common.position_sync.MixinServerGamePacketListenerImpl#teleport(double, double, double, float, float, Set)}
     * the player's dimension will be correct
     */
    @Redirect(
        method = "respawn",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;restoreFrom(Lnet/minecraft/server/level/ServerPlayer;Z)V"
        )
    )
    private void onRestoreFrom(ServerPlayer newPlayer, ServerPlayer that, boolean keepEverything) {
        newPlayer.restoreFrom(that, keepEverything);
        
        newPlayer.connection.player = newPlayer;
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
        ChunkPos chunkPos = new ChunkPos(BlockPos.containing(new Vec3(x, y, z)));
        
        var recs =
            NewChunkTrackingGraph.getPlayerWatchListRecord(dimension, chunkPos.x, chunkPos.z);
        
        if (recs == null) {
            return;
        }
        
        for (NewChunkTrackingGraph.PlayerWatchRecord rec : recs.values()) {
            if (rec.isLoadedToPlayer && rec.player != excludingPlayer) {
                if (NewChunkTrackingGraph.isPlayerWatchingChunkWithinRadius(
                    rec.player, dimension, chunkPos.x, chunkPos.z, (int) distance + 16
                )) {
                    rec.player.connection.send(
                        PacketRedirection.createRedirectedMessage(
                            dimension, (Packet<ClientGamePacketListener>) packet
                        )
                    );
                }
            }
        }
    }
}
