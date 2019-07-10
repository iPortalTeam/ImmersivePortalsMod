package com.qouteall.immersive_portals.exposer;

import net.minecraft.client.network.ClientPlayNetworkHandler;

public interface IEClientWorld {
    public ClientPlayNetworkHandler getNetHandler();
    
    public void setNetHandler(ClientPlayNetworkHandler handler);
}
