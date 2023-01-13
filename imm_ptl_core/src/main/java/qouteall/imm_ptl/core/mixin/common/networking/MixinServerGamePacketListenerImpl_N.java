package qouteall.imm_ptl.core.mixin.common.networking;

import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.platform_specific.IPNetworking;

@Mixin(ServerGamePacketListenerImpl.class)
public class MixinServerGamePacketListenerImpl_N {
    @Shadow
    public ServerPlayer player;
    
    @Inject(
        method = "handleCustomPayload",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onHandleCustomPayload(ServerboundCustomPayloadPacket packet, CallbackInfo ci) {
        boolean handled = IPNetworking.handleImmPtlCorePacketServerSide(
            packet.getIdentifier(), player, packet.getData()
        );
        if (handled) {
            ci.cancel();
        }
    }
}
