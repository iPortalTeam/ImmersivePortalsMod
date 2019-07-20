package com.qouteall.immersive_portals.mixin;

import com.qouteall.immersive_portals.Globals;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class MixinClientPlayerEntity {
    @Inject(
        method = "Lnet/minecraft/client/network/ClientPlayerEntity;sendMovementPackets()V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onSendPositionPacket(CallbackInfo ci) {
        if (!Globals.clientTeleportationManager.shouldSendPositionPacket.getAsBoolean()) {
            ci.cancel();
        }
    }
}
