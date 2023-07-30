package qouteall.imm_ptl.core.mixin.common.debug;

import org.spongepowered.asm.mixin.Mixin;

@Mixin(targets = "net.minecraft.server.level.ServerChunkCache$MainThreadExecutor")
public class MixinServerChunkCacheMainThreadExecutor {
//    private static final RateStat RATE_STAT = new RateStat("serverChunkCacheMainThreadExecutorPoolTask");
    
//    @Inject(
//        method = "pollTask", at = @At("HEAD")
//    )
//    private void onPoolTask(CallbackInfoReturnable<Boolean> cir) {
//        RATE_STAT.hit();
//    }
}
