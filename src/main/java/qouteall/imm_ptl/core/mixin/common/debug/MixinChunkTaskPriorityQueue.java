package qouteall.imm_ptl.core.mixin.common.debug;

import net.minecraft.server.level.ChunkTaskPriorityQueue;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ChunkTaskPriorityQueue.class)
public class MixinChunkTaskPriorityQueue<T> {
//    @Shadow
//    @Final
//    private LongSet acquired;
//
//    @Shadow
//    @Final
//    private int maxTasks;
    
//    private static final RateStat RATE_STAT = new RateStat("chunkTaskPriorityQueuePopThrottled");
//
//    @Inject(
//        method = "pop", at = @At("HEAD")
//    )
//    private void onPop(CallbackInfoReturnable<@Nullable Stream<Either<T, Runnable>>> cir) {
//        if (this.acquired.size() >= this.maxTasks) {
//            RATE_STAT.hit();
//        }
//    }
//
//    @Inject(
//        method = "method_17612",
//        at = @At("HEAD")
//    )
//    private void onWhatever(long l, CallbackInfo ci) {
//        DebugUtil.actualAcquire.incrementAndGet();
//    }
}
