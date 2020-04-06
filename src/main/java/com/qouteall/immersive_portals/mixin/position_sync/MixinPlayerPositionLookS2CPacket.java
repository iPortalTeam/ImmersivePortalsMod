package com.qouteall.immersive_portals.mixin.position_sync;

import com.qouteall.immersive_portals.ducks.IEPlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
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
    
    @Inject(method = "read", at = @At("HEAD"))
    private void onRead(PacketByteBuf packetByteBuf_1, CallbackInfo ci) {
        try {
            playerDimension = DimensionType.byRawId(packetByteBuf_1.readInt());
        }
        catch (IndexOutOfBoundsException e) {
            throw new RuntimeException("The server doesn't install Immmersive Portals Mod");
        }
    }
    
    @Inject(method = "write", at = @At("HEAD"))
    private void onWrite(PacketByteBuf packetByteBuf_1, CallbackInfo ci) {
        packetByteBuf_1.writeInt(playerDimension.getRawId());
    }
}
