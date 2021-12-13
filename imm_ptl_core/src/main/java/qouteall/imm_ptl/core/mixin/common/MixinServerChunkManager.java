package qouteall.imm_ptl.core.mixin.common;

import net.minecraft.server.world.ChunkTicketManager;
import net.minecraft.server.world.ServerChunkManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import qouteall.imm_ptl.core.ducks.IEServerChunkManager;

@Mixin(ServerChunkManager.class)
public abstract class MixinServerChunkManager implements IEServerChunkManager {
    @Shadow
    @Final
    private ChunkTicketManager ticketManager;
    
    @Override
    public ChunkTicketManager getTicketManager() {
        return ticketManager;
    }
}
