package qouteall.imm_ptl.core.mixin.common.networking;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.ClientCommonPacketListener;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import qouteall.imm_ptl.core.ducks.IECustomPayloadPacket;
import qouteall.imm_ptl.core.network.PacketRedirection;

@Mixin(ClientboundCustomPayloadPacket.class)
public class MixinClientboundCustomPayloadPacket implements IECustomPayloadPacket {
    
    @Shadow
    @Final
    private CustomPacketPayload payload;
    
    @Inject(
        method = "readPayload",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void onReadPayload(
        ResourceLocation resourceLocation, FriendlyByteBuf friendlyByteBuf,
        CallbackInfoReturnable<CustomPacketPayload> cir
    ) {
        if (resourceLocation.equals(PacketRedirection.payloadId)) {
            PacketRedirection.Payload p = PacketRedirection.Payload.read(friendlyByteBuf);
            cir.setReturnValue(p);
            cir.cancel();
        }
    }
    
    // this is run before Fabric API try to handle the packet
    @Inject(
        method = "handle(Lnet/minecraft/network/protocol/common/ClientCommonPacketListener;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onHandle(ClientCommonPacketListener clientCommonPacketListener, CallbackInfo ci) {
        if (payload instanceof PacketRedirection.Payload redirectPayload) {
            if (clientCommonPacketListener instanceof ClientGamePacketListener clientGamePacketListener) {
                redirectPayload.handle(clientGamePacketListener);
            }
            
            ci.cancel();
        }
    }
}
