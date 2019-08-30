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
    
    @Shadow
    private boolean field_3698;
    
    @Shadow
    private MinecraftClient client;
    
    @Override
    public void setWorld(ClientWorld world) {
        this.world = world;
    }
    
    @Inject(
        method = "onPlayerPositionLook",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/util/ThreadExecutor;)V",
            shift = At.Shift.AFTER
        ),
        cancellable = true
    )
    private void onProcessingPosistionPacket(
        PlayerPositionLookS2CPacket packet,
        CallbackInfo ci
    ) {
        DimensionType playerDimension = ((IEPlayerPositionLookS2CPacket) packet).getPlayerDimension();
        assert playerDimension != null;
        ClientWorld world = client.world;

//        if (!this.field_3698) {
//            Helper.log("Early position packet received");
//            if (playerDimension != world.dimension.getType()) {
//                CGlobal.clientTeleportationManager.acceptSynchronizationDataFromServer(
//                    playerDimension,
//                    new Vec3d(packet.getX(), packet.getY(), packet.getZ()),
//                    true
//                );
//            }
//            return;
//        }
        
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
