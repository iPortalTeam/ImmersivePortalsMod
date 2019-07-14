package com.qouteall.immersive_portals.exposer;

import net.minecraft.server.world.ServerLightingProvider;
import net.minecraft.server.world.ServerWorld;

public interface IEThreadedAnvilChunkStorage {
    int getWatchDistance();
    
    ServerWorld getWorld();
    
    ServerLightingProvider getLightingProvider();
}
