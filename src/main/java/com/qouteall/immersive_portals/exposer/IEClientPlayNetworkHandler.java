package com.qouteall.immersive_portals.exposer;

import net.minecraft.client.world.ClientWorld;

import java.util.Map;

public interface IEClientPlayNetworkHandler {
    void setWorld(ClientWorld world);
    
    Map getPlayerListEntries();
    
    void setPlayerListEntries(Map value);
}
