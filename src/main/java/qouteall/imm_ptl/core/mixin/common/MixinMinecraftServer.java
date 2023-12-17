package qouteall.imm_ptl.core.mixin.common;

import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.ducks.IEMinecraftServer;
import qouteall.q_misc_util.my_util.MyTaskList;

@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer implements IEMinecraftServer {
    @Unique
    private MyTaskList ip_serverTaskList = new MyTaskList();
    
    @Inject(
        method = "Lnet/minecraft/server/MinecraftServer;runServer()V",
        at = @At("RETURN")
    )
    private void onServerClose(CallbackInfo ci) {
        IPGlobal.SERVER_CLEANUP_EVENT.invoker().accept((MinecraftServer) (Object) this);
    }
    
    @Override
    public MyTaskList ip_getServerTaskList() {
        return ip_serverTaskList;
    }
}
