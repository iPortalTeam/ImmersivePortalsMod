package qouteall.imm_ptl.core.mixin.common.debug;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.server.level.ServerChunkCache$MainThreadExecutor")
public class MixinServerChunkCacheMainThreadExecutor {
//    private static final RateStat RATE_STAT = new RateStat("serverChunkCacheMainThreadExecutorPoolTask");
    
    @Inject(
        method = "pollTask", at = @At("HEAD")
    )
    private void onPoolTask(CallbackInfoReturnable<Boolean> cir) {
//        RATE_STAT.hit();
    }
}
