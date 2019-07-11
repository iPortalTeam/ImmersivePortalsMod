package com.qouteall.immersive_portals.mixin;

import com.qouteall.immersive_portals.Globals;
import com.qouteall.immersive_portals.exposer.IEClientPlayNetworkHandler;
import com.qouteall.immersive_portals.my_util.Helper;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.packet.PlayerPositionLookS2CPacket;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class MixinClientPlayNetworkHandler implements IEClientPlayNetworkHandler {
    @Shadow
    private ClientWorld world;
    
    @Override
    public void setWorld(ClientWorld world) {
        this.world = world;
    }
    
    @Inject(
        method = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;onPlayerPositionLook(Lnet/minecraft/client/network/packet/PlayerPositionLookS2CPacket;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onProcessingPosistionPacket(
        PlayerPositionLookS2CPacket playerPositionLookS2CPacket_1,
        CallbackInfo ci
    ) {
        if (Globals.clientTeleportationManager.shouldIgnorePositionPacket.getAsBoolean()) {
            Helper.log("Position Packet Ignored");
            ci.cancel();
        }
    }
}
