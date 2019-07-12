package com.qouteall.immersive_portals.exposer;

import net.minecraft.server.world.ServerWorld;

public interface IEThreadedAnvilChunkStorage {
    public int getWatchDistance();
    
    public ServerWorld getWorld();
}
