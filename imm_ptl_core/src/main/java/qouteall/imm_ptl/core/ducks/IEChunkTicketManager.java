package qouteall.imm_ptl.core.ducks;

import net.minecraft.server.level.Ticket;
import net.minecraft.util.SortedArraySet;

public interface IEChunkTicketManager {
    void mySetWatchDistance(int newWatchDistance);
    
    SortedArraySet<Ticket<?>> portal_getTicketSet(long chunkPos);
}
