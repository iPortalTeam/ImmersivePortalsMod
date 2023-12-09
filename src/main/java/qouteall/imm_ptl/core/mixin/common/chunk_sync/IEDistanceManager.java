package qouteall.imm_ptl.core.mixin.common.chunk_sync;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.server.level.ChunkTaskPriorityQueueSorter;
import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.Ticket;
import net.minecraft.util.SortedArraySet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.concurrent.Executor;

@Mixin(DistanceManager.class)
public interface IEDistanceManager {
    @Accessor("tickets")
    Long2ObjectOpenHashMap<SortedArraySet<Ticket<?>>> ip_getTickets();
    
    @Accessor("mainThreadExecutor")
    Executor ip_getMainThreadExecutor();
    
    @Accessor("ticketThrottler")
    ChunkTaskPriorityQueueSorter ip_getTicketThrottler();
}
