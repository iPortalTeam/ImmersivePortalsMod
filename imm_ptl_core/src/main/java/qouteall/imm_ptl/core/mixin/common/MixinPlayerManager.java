package qouteall.imm_ptl.core.mixin.common;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
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

@Mixin(PlayerManager.class)
public class MixinPlayerManager {
    @Shadow
    @Final
    private List<ServerPlayerEntity> players;
    
    @Shadow
    @Final
    private MinecraftServer server;
    
    @Inject(
        method = "onPlayerConnect",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/packet/s2c/play/GameJoinS2CPacket;<init>(IZLnet/minecraft/world/GameMode;Lnet/minecraft/world/GameMode;Ljava/util/Set;Lnet/minecraft/util/registry/DynamicRegistryManager$Impl;Lnet/minecraft/world/dimension/DimensionType;Lnet/minecraft/util/registry/RegistryKey;JIIIZZZZ)V"
        )
    )
    private void onConnectionEstablished(
        ClientConnection connection,
        ServerPlayerEntity player,
        CallbackInfo ci
    ) {
        player.networkHandler.sendPacket(IPNetworking.createDimSync());
    }
    
    @Inject(method = "sendWorldInfo", at = @At("RETURN"))
    private void onSendWorldInfo(ServerPlayerEntity player, ServerWorld world, CallbackInfo ci) {
        if (!IPGlobal.serverTeleportationManager.isFiringMyChangeDimensionEvent) {
            GlobalPortalStorage.onPlayerLoggedIn(player);
        }
    }
    
    @Inject(method = "onPlayerConnect", at = @At("TAIL"))
    private void onOnPlayerConnect(ClientConnection connection, ServerPlayerEntity player, CallbackInfo ci) {
        NewChunkTrackingGraph.updateForPlayer(player);
    }
    
    //with redirection
    @Inject(
        method = "sendToDimension",
        at = @At("HEAD"),
        cancellable = true
    )
    public void sendToDimension(Packet<?> packet, RegistryKey<World> dimension, CallbackInfo ci) {
        for (ServerPlayerEntity player : players) {
            if (player.world.getRegistryKey() == dimension) {
                player.networkHandler.sendPacket(
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
    public void sendToAround(
        @Nullable PlayerEntity excludingPlayer,
        double x, double y, double z, double distance,
        RegistryKey<World> dimension, Packet<?> packet
    ) {
        ChunkPos chunkPos = new ChunkPos(new BlockPos(new Vec3d(x, y, z)));
        
        NewChunkTrackingGraph.getPlayersViewingChunk(
            dimension, chunkPos.x, chunkPos.z
        ).filter(playerEntity -> NewChunkTrackingGraph.isPlayerWatchingChunkWithinRaidus(
            playerEntity, dimension, chunkPos.x, chunkPos.z, (int) distance + 16
        )).forEach(playerEntity -> {
            if (playerEntity != excludingPlayer) {
                playerEntity.networkHandler.sendPacket(IPNetworking.createRedirectedMessage(
                    dimension, packet
                ));
            }
        });
    }
}
