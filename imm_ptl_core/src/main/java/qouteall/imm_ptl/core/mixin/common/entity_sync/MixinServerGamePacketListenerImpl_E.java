package qouteall.imm_ptl.core.mixin.common.entity_sync;

import net.minecraft.network.protocol.Packet;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import qouteall.imm_ptl.core.network.PacketRedirection;

@Mixin(ServerGamePacketListenerImpl.class)
public class MixinServerGamePacketListenerImpl_E {
    @ModifyVariable(
        method = "send(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;)V",
        at = @At("HEAD"),
        argsOnly = true
    )
    private Packet modifyPacket(Packet originalPacket) {
        if (PacketRedirection.getForceRedirectDimension() == null) {
            return originalPacket;
        }
        
        return PacketRedirection.createRedirectedMessage(PacketRedirection.getForceRedirectDimension(), originalPacket);
    }
}
