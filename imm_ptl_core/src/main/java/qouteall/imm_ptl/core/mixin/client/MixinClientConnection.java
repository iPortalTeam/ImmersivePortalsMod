package qouteall.imm_ptl.core.mixin.client;

import io.netty.channel.Channel;
import net.minecraft.network.ClientConnection;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public class MixinClientConnection {
    @Shadow
    private Channel channel;
    
    //avoid crashing by npe
    @Inject(method = "disconnect", at = @At("HEAD"), cancellable = true)
    private void onBeforeDisconnect(Text text_1, CallbackInfo ci) {
        if (channel == null) {
            ci.cancel();
        }
    }
}
