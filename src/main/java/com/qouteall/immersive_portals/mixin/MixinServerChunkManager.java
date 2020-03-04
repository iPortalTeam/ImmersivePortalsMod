package com.qouteall.immersive_portals.mixin;

import com.qouteall.immersive_portals.ducks.IEServerChunkManager;
import net.minecraft.server.world.ChunkTicketManager;
import net.minecraft.server.world.ServerChunkManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

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
