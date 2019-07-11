package com.qouteall.immersive_portals.mixin;

import com.qouteall.immersive_portals.Globals;
import com.qouteall.immersive_portals.chunk_loading.RedirectedMessageManager;
import com.qouteall.immersive_portals.exposer.IEChunkHolder;
import net.minecraft.network.Packet;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Mixin(ChunkHolder.class)
public class MixinChunkHolder implements IEChunkHolder {
    //not shadow
    private DimensionType dimension;
    
    @Shadow
    @Final
    private ChunkPos pos;
    
    @Shadow
    @Final
    private ChunkHolder.PlayersWatchingChunkProvider playersWatchingChunkProvider;
    
    @Inject(
        method = "Lnet/minecraft/server/world/ChunkHolder;sendPacketToPlayersWatching(Lnet/minecraft/network/Packet;Z)V",
        at = @At("TAIL")
    )
    private void onSendPacketToPlayersWatching(
        Packet<?> packet_1,
        boolean boolean_1,
        CallbackInfo ci
    ) {
        //TODO release this
        //assert dimension != null;
        
        Set<ServerPlayerEntity> vanillaWatchers =
            this.playersWatchingChunkProvider.getPlayersWatchingChunk(
                this.pos, boolean_1
            ).collect(Collectors.toSet());
        Collection<ServerPlayerEntity> myWatchers = Globals.chunkTracker
            .getPlayersViewingChunk(dimension, pos);
        myWatchers.stream().filter(
            player -> !vanillaWatchers.contains(player)
        ).forEach(
            playerEntity -> {
                playerEntity.networkHandler.sendPacket(
                    RedirectedMessageManager.createRedirectedMessage(
                        dimension, packet_1
                    )
                );
            }
        );
    }
    
    @Override
    public DimensionType getDimension() {
        return dimension;
    }
    
    @Override
    public void setDimension(DimensionType dimension_) {
        dimension = dimension_;
    }
}
