package qouteall.imm_ptl.core.ducks;

import net.minecraft.server.level.Ticket;
import java.util.List;

public interface IEDistanceManager {
    
    List<Ticket> portal_getTicketSet(long chunkPos);
    
    
}
