package qouteall.imm_ptl.core.mixin.common.entity_sync;

import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.network.PacketRedirection;

@Mixin(ServerCommonPacketListenerImpl.class)
public class MixinServerGamePacketListenerImpl_Redirect {
    @Shadow @Final protected MinecraftServer server;
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    @ModifyVariable(
        method = "send(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;)V",
        at = @At("HEAD"),
        argsOnly = true
    )
    private Packet modifyPacket(Packet originalPacket) {
        if (PacketRedirection.getForceRedirectDimension() == null) {
            return originalPacket;
        }
        
        return PacketRedirection.createRedirectedMessage(
            server,
            PacketRedirection.getForceRedirectDimension(),
            originalPacket
        );
    }
    
    @SuppressWarnings("unchecked")
    @Inject(
        method = "send(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/Connection;send(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;Z)V"
        ),
        cancellable = true
    )
    private void onSend(
        Packet<?> packet, @Nullable PacketSendListener packetSendListener, CallbackInfo ci
    ) {
        PacketRedirection.ForceBundleCallback forceBundleCallback = PacketRedirection.getForceBundleCallback();
        if (forceBundleCallback != null) {
            forceBundleCallback.accept(
                (ServerCommonPacketListenerImpl) (Object) this,
                (Packet<ClientGamePacketListener>) packet
            );
            ci.cancel();
        }
    }
}
