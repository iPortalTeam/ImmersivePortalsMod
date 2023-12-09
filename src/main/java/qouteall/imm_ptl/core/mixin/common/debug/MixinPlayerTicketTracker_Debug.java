package qouteall.imm_ptl.core.mixin.common.debug;

import org.spongepowered.asm.mixin.Mixin;

@Mixin(targets = "net.minecraft.server.level.DistanceManager$PlayerTicketTracker")
public class MixinPlayerTicketTracker_Debug {
//    private static final RateStat debugRateStat = new RateStat("addPlayerTicket");
    
//    @Inject(
//        method = "method_17667",
//        at = @At(
//            value = "INVOKE",
//            target = "Lnet/minecraft/server/level/DistanceManager;addTicket(JLnet/minecraft/server/level/Ticket;)V"
//        )
//    )
//    private void onAddTicket(long l, Ticket ticket, CallbackInfo ci) {
//        debugRateStat.hit();
//    }
//
//    @Inject(
//        method = "onLevelChange(JIZZ)V",
//        at = @At(
//            value = "INVOKE",
//            target = "Lnet/minecraft/server/level/ChunkTaskPriorityQueueSorter;message(Ljava/lang/Runnable;JLjava/util/function/IntSupplier;)Lnet/minecraft/server/level/ChunkTaskPriorityQueueSorter$Message;"
//        )
//    )
//    private void onMessage(long chunkPos, int i, boolean bl, boolean bl2, CallbackInfo ci) {
//        DebugUtil.acquireCounter++;
//    }
}
