package qouteall.imm_ptl.core.mixin.common;

import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.IPPerServerInfo;
import qouteall.imm_ptl.core.ducks.IEMinecraftServer;

@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer implements IEMinecraftServer {
    @Unique
    IPPerServerInfo ipPerServerInfo = new IPPerServerInfo();
    
    @Inject(
        method = "Lnet/minecraft/server/MinecraftServer;runServer()V",
        at = @At("RETURN")
    )
    private void onServerClose(CallbackInfo ci) {
        IPGlobal.SERVER_CLEANUP_EVENT.invoker().accept((MinecraftServer) (Object) this);
    }
    
    @Override
    public IPPerServerInfo ip_getPerServerInfo() {
        return ipPerServerInfo;
    }
}
