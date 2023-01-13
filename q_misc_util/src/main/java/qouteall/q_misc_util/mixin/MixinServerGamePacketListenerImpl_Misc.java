package qouteall.q_misc_util.mixin;

import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.q_misc_util.MiscNetworking;

@Mixin(ServerGamePacketListenerImpl.class)
public class MixinServerGamePacketListenerImpl_Misc {
    @Shadow
    public ServerPlayer player;
    
    @Inject(
        method = "handleCustomPayload",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onHandleCustomPayload(ServerboundCustomPayloadPacket packet, CallbackInfo ci) {
        boolean handled = MiscNetworking.handleMiscUtilPacketServerSide(
            packet.getIdentifier(), player, packet.getData()
        );
        if (handled) {
            ci.cancel();
        }
    }
}
