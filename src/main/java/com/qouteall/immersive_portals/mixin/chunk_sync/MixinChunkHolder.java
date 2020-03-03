package com.qouteall.immersive_portals.mixin.chunk_sync;

import com.qouteall.hiding_in_the_bushes.MyNetwork;
import com.qouteall.immersive_portals.chunk_loading.NewChunkTrackingGraph;
import com.qouteall.immersive_portals.ducks.IEChunkHolder;
import com.qouteall.immersive_portals.ducks.IEThreadedAnvilChunkStorage;
import net.minecraft.network.Packet;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

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
     */
    @Overwrite
    private void sendPacketToPlayersWatching(Packet<?> packet_1, boolean boolean_1) {
        DimensionType dimension =
            ((IEThreadedAnvilChunkStorage) playersWatchingChunkProvider).getWorld().dimension.getType();
    
        NewChunkTrackingGraph.getPlayersViewingChunk(
            dimension, pos.x, pos.z
        ).forEach(player ->
            player.networkHandler.sendPacket(
                MyNetwork.createRedirectedMessage(
                    dimension, packet_1
                )
            )
        );
    
    }
    
}
