package com.qouteall.immersive_portals.ducks;

import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.ChunkSectionPos;

public interface IEEntityTracker {
    Entity getEntity_();
    
    void updateEntityTrackingStatus(ServerPlayerEntity player);
    
    void onPlayerRespawn(ServerPlayerEntity oldPlayer);
    
    void resendSpawnPacketToTrackers();
    
    void stopTrackingToAllPlayers_();
    
    void tickEntry();
    
    ChunkSectionPos getLastCameraPosition();
    
    void setLastCameraPosition(ChunkSectionPos arg);
}
