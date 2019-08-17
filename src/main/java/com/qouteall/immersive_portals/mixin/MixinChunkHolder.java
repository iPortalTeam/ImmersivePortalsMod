package com.qouteall.immersive_portals.mixin;

import com.google.common.collect.Streams;
import com.qouteall.immersive_portals.MyNetwork;
import com.qouteall.immersive_portals.SGlobal;
import com.qouteall.immersive_portals.exposer.IEChunkHolder;
import com.qouteall.immersive_portals.exposer.IEThreadedAnvilChunkStorage;
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
        
        Streams.concat(
            this.playersWatchingChunkProvider.getPlayersWatchingChunk(
                this.pos, boolean_1
            ),
            SGlobal.chunkTracker.getPlayersViewingChunk(dimension, pos)
        ).distinct().forEach(player ->
            player.networkHandler.sendPacket(
                MyNetwork.createRedirectedMessage(
                    dimension, packet_1
                )
            )
        );
        
    }
    
}
