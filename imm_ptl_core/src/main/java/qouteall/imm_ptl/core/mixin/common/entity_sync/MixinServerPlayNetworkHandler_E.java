package qouteall.imm_ptl.core.mixin.common.entity_sync;

import qouteall.imm_ptl.core.platform_specific.MyNetwork;
import qouteall.imm_ptl.core.network.CommonNetwork;
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
