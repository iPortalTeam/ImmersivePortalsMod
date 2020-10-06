package com.qouteall.immersive_portals.ducks;

import com.qouteall.immersive_portals.portal.global_portals.GlobalTrackedPortal;
import net.minecraft.client.network.ClientPlayNetworkHandler;

import javax.annotation.Nullable;
import java.util.List;

public interface IEClientWorld {
    ClientPlayNetworkHandler getNetHandler();
    
    void setNetHandler(ClientPlayNetworkHandler handler);
    
    @Nullable
    List<GlobalTrackedPortal> getGlobalPortals();
    
    void setGlobalPortals(List<GlobalTrackedPortal> arg);
}
