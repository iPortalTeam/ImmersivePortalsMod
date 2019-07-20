package com.qouteall.immersive_portals.mixin;

import com.qouteall.immersive_portals.exposer.IEPlayerMoveC2SPacket;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.network.packet.PlayerMoveC2SPacket;
import net.minecraft.util.PacketByteBuf;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerMoveC2SPacket.class)
public class MixinPlayerMoveC2SPacket implements IEPlayerMoveC2SPacket {
    private DimensionType playerDimension;
    
    @Inject(
        method = "Lnet/minecraft/server/network/packet/PlayerMoveC2SPacket;read(Lnet/minecraft/util/PacketByteBuf;)V",
        at = @At("HEAD")
    )
    private void onRead(PacketByteBuf packetByteBuf_1, CallbackInfo ci) {
        playerDimension = DimensionType.byRawId(packetByteBuf_1.readInt());
    }
    
    @Inject(
        method = "Lnet/minecraft/server/network/packet/PlayerMoveC2SPacket;write(Lnet/minecraft/util/PacketByteBuf;)V",
        at = @At("HEAD")
    )
    private void onWrite(PacketByteBuf packetByteBuf_1, CallbackInfo ci) {
        //this must be invoke on client
        playerDimension = MinecraftClient.getInstance().player.dimension;
        
        packetByteBuf_1.writeInt(playerDimension.getRawId());
    }
    
    @Override
    public DimensionType getPlayerDimension() {
        return playerDimension;
    }
}
