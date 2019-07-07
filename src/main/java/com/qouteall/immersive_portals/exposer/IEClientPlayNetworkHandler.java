package com.qouteall.immersive_portals.exposer;

import net.minecraft.client.world.ClientWorld;

public interface IEClientPlayNetworkHandler {
    ClientWorld getWorld();
    
    void setWorld(ClientWorld world);
}
