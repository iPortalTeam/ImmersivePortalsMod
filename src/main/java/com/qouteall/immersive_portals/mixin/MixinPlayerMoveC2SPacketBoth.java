package com.qouteall.immersive_portals.mixin;

import com.qouteall.immersive_portals.exposer.IEPlayerMoveC2SPacket;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.network.packet.PlayerMoveC2SPacket;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerMoveC2SPacket.Both.class)
public class MixinPlayerMoveC2SPacketBoth {
    @Inject(
        method = "Lnet/minecraft/server/network/packet/PlayerMoveC2SPacket$Both;<init>(DDDFFZ)V",
        at = @At("RETURN")
    )
    private void onConstruct2(
        double double_1,
        double double_2,
        double double_3,
        float float_1,
        float float_2,
        boolean boolean_1,
        CallbackInfo ci
    ) {
        DimensionType dimension = MinecraftClient.getInstance().player.dimension;
        ((IEPlayerMoveC2SPacket) this).setPlayerDimension(dimension);
        assert dimension == MinecraftClient.getInstance().world.dimension.getType();
    }
}
