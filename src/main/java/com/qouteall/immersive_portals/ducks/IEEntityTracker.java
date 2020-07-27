package com.qouteall.immersive_portals.ducks;

import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;

public interface IEEntityTracker {
    Entity getEntity_();
    
    void updateEntityTrackingStatus(ServerPlayerEntity player);
    
    void onPlayerRespawn(ServerPlayerEntity oldPlayer);
    
    void resendSpawnPacketToTrackers();
    
    void stopTrackingToAllPlayers_();
}
