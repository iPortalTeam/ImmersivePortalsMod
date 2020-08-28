package com.qouteall.immersive_portals.mixin.client.sync;

import com.qouteall.immersive_portals.ducks.IEPlayerMoveC2SPacket;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(PlayerMoveC2SPacket.LookOnly.class)
public class MixinPlayerMoveC2SPacketLookOnly {
    @Environment(EnvType.CLIENT)
    @Inject(
        method = "<init>(FFZ)V",
        at = @At("RETURN")
    )
    private void onConstruct(float float_1, float float_2, boolean boolean_1, CallbackInfo ci) {
        RegistryKey<World> dimension = MinecraftClient.getInstance().player.world.getRegistryKey();
        ((IEPlayerMoveC2SPacket) this).setPlayerDimension(dimension);
        assert dimension == MinecraftClient.getInstance().world.getRegistryKey();
    }
    
    
}
