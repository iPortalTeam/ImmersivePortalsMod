package com.qouteall.immersive_portals.mixin.position_sync;

import com.qouteall.immersive_portals.dimension_sync.DimId;
import com.qouteall.immersive_portals.ducks.IEPlayerMoveC2SPacket;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerMoveC2SPacket.class)
public class MixinPlayerMoveC2SPacket_S implements IEPlayerMoveC2SPacket {
    private RegistryKey<World> playerDimension;
    
    @Inject(
        method = "read",
        at = @At("HEAD")
    )
    private void onRead(PacketByteBuf buf, CallbackInfo ci) {
        playerDimension = DimId.readWorldId(buf, false);
    }
    
    @Inject(
        method = "write",
        at = @At("HEAD")
    )
    private void onWrite(PacketByteBuf buf, CallbackInfo ci) {
        DimId.writeWorldId(buf, playerDimension, true);
    }
    
    @Override
    public RegistryKey<World> getPlayerDimension() {
        return playerDimension;
    }
    
    @Override
    public void setPlayerDimension(RegistryKey<World> dim) {
        playerDimension = dim;
    }
    
    
}
