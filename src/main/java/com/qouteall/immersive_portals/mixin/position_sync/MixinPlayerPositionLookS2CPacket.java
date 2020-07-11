package com.qouteall.immersive_portals.mixin.position_sync;

import com.qouteall.immersive_portals.dimension_sync.DimId;
import com.qouteall.immersive_portals.ducks.IEPlayerPositionLookS2CPacket;
import net.fabricmc.api.EnvType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerPositionLookS2CPacket.class)
public class MixinPlayerPositionLookS2CPacket implements IEPlayerPositionLookS2CPacket {
    private RegistryKey<World> playerDimension;
    
    @Override
    public RegistryKey<World> getPlayerDimension() {
        return playerDimension;
    }
    
    @Override
    public void setPlayerDimension(RegistryKey<World> dimension) {
        playerDimension = dimension;
    }
    
    @Inject(method = "read", at = @At("HEAD"))
    private void onRead(PacketByteBuf packetByteBuf_1, CallbackInfo ci) {
        try {
            playerDimension = DimId.readWorldId(packetByteBuf_1, true);
        }
        catch (IndexOutOfBoundsException e) {
            throw new RuntimeException("The server doesn't install Immmersive Portals Mod");
        }
    }
    
    @Inject(method = "write", at = @At("HEAD"))
    private void onWrite(PacketByteBuf packetByteBuf_1, CallbackInfo ci) {
        DimId.writeWorldId(packetByteBuf_1, playerDimension, false);
    }
}
