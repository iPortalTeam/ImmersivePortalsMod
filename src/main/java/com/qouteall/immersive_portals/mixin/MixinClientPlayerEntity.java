package com.qouteall.immersive_portals.mixin;

import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ClientPlayerEntity.class)
public class MixinClientPlayerEntity {
//    @Inject(
//        method = "Lnet/minecraft/client/network/ClientPlayerEntity;sendMovementPackets()V",
//        at = @At("HEAD"),
//        cancellable = true
//    )
//    private void onSendPositionPacket(CallbackInfo ci) {
//        if (!Globals.clientTeleportationManager.shouldSendPositionPacket.getAsBoolean()) {
//            ci.cancel();
//        }
//    }
}
