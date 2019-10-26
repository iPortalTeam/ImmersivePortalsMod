package com.qouteall.immersive_portals.ducks;

import com.qouteall.immersive_portals.portal.global_portals.GlobalTrackedPortal;
import net.minecraft.client.network.ClientPlayNetworkHandler;

import java.util.List;

public interface IEClientWorld {
    public ClientPlayNetworkHandler getNetHandler();
    
    public void setNetHandler(ClientPlayNetworkHandler handler);
    
    public List<GlobalTrackedPortal> getGlobalPortals();
    
    public void setGlobalPortals(List<GlobalTrackedPortal> arg);
}
