package qouteall.imm_ptl.core.mixin.client;

import io.netty.channel.Channel;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public class MixinClientConnection {
    @Shadow
    private Channel channel;
    
    //avoid crashing by npe
    @Inject(method = "Lnet/minecraft/network/Connection;disconnect(Lnet/minecraft/network/chat/Component;)V", at = @At("HEAD"), cancellable = true)
    private void onBeforeDisconnect(Component text_1, CallbackInfo ci) {
        if (channel == null) {
            ci.cancel();
        }
    }
}
