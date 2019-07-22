package com.qouteall.immersive_portals.exposer;

import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;

public interface IEEntityTracker {
    Entity getEntity_();
    
    void updateCameraPosition_(ServerPlayerEntity player);
    
    void onPlayerRespawn(ServerPlayerEntity oldPlayer);
}
