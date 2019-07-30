package com.qouteall.immersive_portals.mixin_client;

import com.qouteall.immersive_portals.exposer.IEClientPlayNetworkHandler;
import com.qouteall.immersive_portals.exposer.IEPlayerPositionLookS2CPacket;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.packet.PlayerPositionLookS2CPacket;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.dimension.DimensionType;
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
        PlayerPositionLookS2CPacket packet,
        CallbackInfo ci
    ) {
        DimensionType playerDimension = ((IEPlayerPositionLookS2CPacket) packet).getPlayerDimension();
        assert playerDimension != null;
        ClientWorld world = MinecraftClient.getInstance().world;
        if (world != null) {
            if (world.dimension != null) {
                if (world.dimension.getType() != playerDimension) {
                    if (!MinecraftClient.getInstance().player.removed) {
                        ci.cancel();
                    }
                }
            }
        }
        
    }
}
