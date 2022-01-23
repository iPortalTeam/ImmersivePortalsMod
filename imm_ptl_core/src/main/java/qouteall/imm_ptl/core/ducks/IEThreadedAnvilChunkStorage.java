package qouteall.imm_ptl.core.ducks;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;

public interface IEThreadedAnvilChunkStorage {
    int getWatchDistance();
    
    ServerLevel getWorld();
    
    ThreadedLevelLightEngine getLightingProvider();
    
    ChunkHolder getChunkHolder_(long long_1);
    
    void onPlayerRespawn(ServerPlayer oldPlayer);
    
    void updateEntityTrackersAfterSendingChunkPacket(
        LevelChunk chunk,
        ServerPlayer playerEntity
    );
    
    void resendSpawnPacketToTrackers(Entity entity);
    
    boolean portal_isChunkGenerated(ChunkPos chunkPos);
    
    Int2ObjectMap<ChunkMap.TrackedEntity> getEntityTrackerMap();
}
