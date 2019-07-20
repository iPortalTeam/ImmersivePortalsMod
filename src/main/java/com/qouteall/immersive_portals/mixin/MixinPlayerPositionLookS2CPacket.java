package com.qouteall.immersive_portals.mixin;

import com.qouteall.immersive_portals.exposer.IEPlayerPositionLookS2CPacket;
import net.minecraft.client.network.packet.PlayerPositionLookS2CPacket;
import net.minecraft.util.PacketByteBuf;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerPositionLookS2CPacket.class)
public class MixinPlayerPositionLookS2CPacket implements IEPlayerPositionLookS2CPacket {
    private DimensionType playerDimension;
    
    @Override
    public DimensionType getPlayerDimension() {
        return playerDimension;
    }
    
    @Override
    public void setPlayerDimension(DimensionType dimension) {
        playerDimension = dimension;
    }
    
    @Inject(
        method = "Lnet/minecraft/client/network/packet/PlayerPositionLookS2CPacket;read(Lnet/minecraft/util/PacketByteBuf;)V",
        at = @At("HEAD")
    )
    private void onRead(PacketByteBuf packetByteBuf_1, CallbackInfo ci) {
        playerDimension = DimensionType.byRawId(packetByteBuf_1.readInt());
    }
    
    @Inject(
        method = "Lnet/minecraft/client/network/packet/PlayerPositionLookS2CPacket;write(Lnet/minecraft/util/PacketByteBuf;)V",
        at = @At("HEAD")
    )
    private void onWrite(PacketByteBuf packetByteBuf_1, CallbackInfo ci) {
        packetByteBuf_1.writeInt(playerDimension.getRawId());
    }
}
