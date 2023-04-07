package qouteall.imm_ptl.core.mixin.client.sync;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.thread.ReentrantBlockableEventLoop;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.imm_ptl.core.miscellaneous.IPVanillaCopy;
import qouteall.imm_ptl.core.network.PacketRedirectionClient;

@Mixin(Minecraft.class)
public abstract class MixinMinecraft_RedirectedPacket extends ReentrantBlockableEventLoop<Runnable> {
    
    public MixinMinecraft_RedirectedPacket(String string) {
        super(string);
    }
    
    // ensure that the task is processed with the redirected dimension
    @Inject(
        method = "Lnet/minecraft/client/Minecraft;wrapRunnable(Ljava/lang/Runnable;)Ljava/lang/Runnable;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onCreateTask(Runnable runnable, CallbackInfoReturnable<Runnable> cir) {
        Minecraft this_ = (Minecraft) (Object) this;
        
        ResourceKey<Level> redirectedDimension = PacketRedirectionClient.clientTaskRedirection.get();
        if (redirectedDimension != null) {
            Runnable newRunnable = () -> {
                ClientWorldLoader.withSwitchedWorldFailSoft(redirectedDimension, runnable);
            };
            cir.setReturnValue(newRunnable);
        }
    }
    
    /**
     * Make sure that the redirected packet handling won't be delayed.
     * If not on thread, it will delay handling.
     * If running task, it will delay handling.
     * If on thread and running task, normally it will delay handling, but this override
     *  makes it to not delay when processing redirected packet.
     */
    @IPVanillaCopy
    @Override
    public boolean scheduleExecutables() {
        boolean onThread = isSameThread();
        
        if (onThread) {
            if (PacketRedirectionClient.getIsProcessingRedirectedMessage()) {
                return false;
            }
        }
        
        return this.runningTask() || !onThread;
    }
}
