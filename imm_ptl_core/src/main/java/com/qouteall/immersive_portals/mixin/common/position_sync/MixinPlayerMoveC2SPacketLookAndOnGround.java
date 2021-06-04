package com.qouteall.immersive_portals.mixin.common.position_sync;

import com.qouteall.immersive_portals.dimension_sync.DimId;
import com.qouteall.immersive_portals.ducks.IEPlayerMoveC2SPacket;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerMoveC2SPacket.LookAndOnGround.class)
public class MixinPlayerMoveC2SPacketLookAndOnGround {
    @Inject(method = "read", at = @At("RETURN"), cancellable = true)
    private static void onRead(
        PacketByteBuf buf, CallbackInfoReturnable<PlayerMoveC2SPacket.LookAndOnGround> cir
    ) {
        RegistryKey<World> playerDim = DimId.readWorldId(buf, false);
        ((IEPlayerMoveC2SPacket) cir.getReturnValue()).setPlayerDimension(playerDim);
    }
}
