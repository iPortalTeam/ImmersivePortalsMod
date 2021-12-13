package qouteall.imm_ptl.core.mixin.common.chunk_sync;

import net.minecraft.network.Packet;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import qouteall.imm_ptl.core.chunk_loading.NewChunkTrackingGraph;
import qouteall.imm_ptl.core.ducks.IEChunkHolder;
import qouteall.imm_ptl.core.ducks.IEThreadedAnvilChunkStorage;
import qouteall.imm_ptl.core.platform_specific.IPNetworking;

import java.util.function.Consumer;

@Mixin(ChunkHolder.class)
public class MixinChunkHolder implements IEChunkHolder {
    
    @Shadow
    @Final
    private ChunkPos pos;
    
    @Shadow
    @Final
    private ChunkHolder.PlayersWatchingChunkProvider playersWatchingChunkProvider;
    
    /**
     * @author qouteall
     * @reason overwriting is clearer
     */
    @Overwrite
    private void sendPacketToPlayersWatching(Packet<?> packet_1, boolean onlyOnRenderDistanceEdge) {
        RegistryKey<World> dimension =
            ((IEThreadedAnvilChunkStorage) playersWatchingChunkProvider).getWorld().getRegistryKey();
        
        Consumer<ServerPlayerEntity> func = player ->
            player.networkHandler.sendPacket(
                IPNetworking.createRedirectedMessage(
                    dimension, packet_1
                )
            );
        
        if (onlyOnRenderDistanceEdge) {
            NewChunkTrackingGraph.getFarWatchers(
                dimension, pos.x, pos.z
            ).forEach(func);
        }
        else {
            NewChunkTrackingGraph.getPlayersViewingChunk(
                dimension, pos.x, pos.z
            ).forEach(func);
        }
        
    }
    
}
