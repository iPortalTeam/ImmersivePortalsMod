package qouteall.imm_ptl.core.mixin.common.chunk_sync;

import net.minecraft.server.level.ChunkTaskPriorityQueueSorter;
import net.minecraft.util.thread.ProcessorMailbox;
import net.minecraft.util.thread.StrictQueue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChunkTaskPriorityQueueSorter.class)
public interface IEChunkTaskPriorityQueueSorter {
    @Accessor("mailbox")
    ProcessorMailbox<StrictQueue.IntRunnable> ip_getMailBox();
}
