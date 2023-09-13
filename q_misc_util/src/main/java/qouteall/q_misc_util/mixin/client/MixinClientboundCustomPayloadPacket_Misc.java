package qouteall.q_misc_util.mixin.client;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.q_misc_util.MiscNetworking;

@Mixin(ClientboundCustomPayloadPacket.class)
public class MixinClientboundCustomPayloadPacket_Misc {
    @Shadow @Final private ResourceLocation identifier;
    
    @Shadow @Final private FriendlyByteBuf data;
    
    // this is run before Fabric API try to handle the packet
    @Inject(
        method = "handle(Lnet/minecraft/network/protocol/game/ClientGamePacketListener;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onHandle(ClientGamePacketListener handler, CallbackInfo ci) {
        // NOTE should not change reader index in `data`
        boolean handled = MiscNetworking.handleMiscUtilPacketClientSide(
            identifier, () -> new FriendlyByteBuf(data.slice()), handler
        );
        if (handled) {
            ci.cancel();
        }
    }
}
