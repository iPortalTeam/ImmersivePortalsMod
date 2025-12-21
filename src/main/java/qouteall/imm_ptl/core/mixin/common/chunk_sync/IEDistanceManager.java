package qouteall.imm_ptl.core.mixin.common.chunk_sync;

import net.minecraft.server.level.DistanceManager;
import net.minecraft.world.level.TicketStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.concurrent.Executor;

@Mixin(DistanceManager.class)
public interface IEDistanceManager {
    @Accessor("mainThreadExecutor")
    Executor ip_getMainThreadExecutor();
    
    @Accessor("ticketStorage")
    TicketStorage ip_getTicketStorage();
}
