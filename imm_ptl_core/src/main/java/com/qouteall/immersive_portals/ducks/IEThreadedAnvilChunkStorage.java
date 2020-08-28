package com.qouteall.immersive_portals.ducks;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerLightingProvider;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;

import java.io.File;

public interface IEThreadedAnvilChunkStorage {
    int getWatchDistance();
    
    ServerWorld getWorld();
    
    ServerLightingProvider getLightingProvider();
    
    ChunkHolder getChunkHolder_(long long_1);
    
    void onPlayerRespawn(ServerPlayerEntity oldPlayer);
    
    void updateEntityTrackersAfterSendingChunkPacket(
        WorldChunk chunk,
        ServerPlayerEntity playerEntity
    );
    
    void resendSpawnPacketToTrackers(Entity entity);
    
    File portal_getSaveDir();
    
    boolean portal_isChunkGenerated(ChunkPos chunkPos);
    
    Int2ObjectMap<ThreadedAnvilChunkStorage.EntityTracker> getEntityTrackerMap();
}
