package com.qouteall.immersive_portals.mixin.common.entity_sync;

import com.qouteall.hiding_in_the_bushes.MyNetwork;
import com.qouteall.immersive_portals.network.CommonNetwork;
import net.minecraft.network.Packet;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ServerPlayNetworkHandler.class)
public class MixinServerPlayNetworkHandler_E {
    @ModifyVariable(
        method = "sendPacket(Lnet/minecraft/network/Packet;Lio/netty/util/concurrent/GenericFutureListener;)V",
        at = @At("HEAD"),
        argsOnly = true
    )
    private Packet modifyPacket(Packet originalPacket) {
        if (CommonNetwork.getForceRedirectDimension() == null) {
            return originalPacket;
        }
        
        return MyNetwork.createRedirectedMessage(CommonNetwork.getForceRedirectDimension(), originalPacket);
    }
}
