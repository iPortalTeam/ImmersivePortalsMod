package com.qouteall.immersive_portals.exposer;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerLightingProvider;
import net.minecraft.server.world.ServerWorld;

public interface IEThreadedAnvilChunkStorage {
    int getWatchDistance();
    
    ServerWorld getWorld();
    
    ServerLightingProvider getLightingProvider();
    
    ChunkHolder getChunkHolder_(long long_1);
    
    void onPlayerRespawn(ServerPlayerEntity oldPlayer);
}
