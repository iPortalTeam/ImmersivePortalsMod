package com.qouteall.immersive_portals.mixin;

import com.qouteall.immersive_portals.exposer.IEChunkHolder;
import net.minecraft.network.Packet;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkHolder.class)
public class MixinChunkHolder implements IEChunkHolder {
    //not shadow
    private DimensionType dimension;
    
    @Shadow
    @Final
    private ChunkPos pos;
    
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
        
//        Globals.chunkSyncingManager
//            .getIndirectViewers(dimension, pos)
//            .forEach(
//                playerEntity -> {
//                    assert playerEntity.dimension != dimension;
//                    playerEntity.networkHandler.sendPacket(
//                        RedirectedMessageManager.createRedirectedMessage(
//                            dimension, packet_1
//                        )
//                    );
//                }
//            );
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
