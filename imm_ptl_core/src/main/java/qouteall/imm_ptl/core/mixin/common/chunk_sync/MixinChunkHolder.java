package qouteall.imm_ptl.core.mixin.common.chunk_sync;

import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
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
    private ChunkHolder.PlayerProvider playerProvider;
    
    /**
     * @author qouteall
     * @reason overwriting is clearer
     */
    @Overwrite
    private void broadcast(Packet<?> packet_1, boolean onlyOnRenderDistanceEdge) {
        ResourceKey<Level> dimension =
            ((IEThreadedAnvilChunkStorage) playerProvider).getWorld().dimension();
        
        Consumer<ServerPlayer> func = player ->
            player.connection.send(
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
