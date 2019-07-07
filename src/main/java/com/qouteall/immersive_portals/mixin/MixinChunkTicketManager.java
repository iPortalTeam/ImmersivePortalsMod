package com.qouteall.immersive_portals.mixin;

import com.qouteall.immersive_portals.exposer.IEChunkTicketManager;
import net.minecraft.server.world.ChunkTicketManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ChunkTicketManager.class)
public class MixinChunkTicketManager implements IEChunkTicketManager {
    @Shadow
    private long location;
    
    @Override
    public long getLocation() {
        return location;
    }
}
