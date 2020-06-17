package com.qouteall.immersive_portals.ducks;

import net.minecraft.util.MetricsData;
import net.minecraft.util.registry.RegistryTracker;

public interface IEMinecraftServer {
    public MetricsData getMetricsDataNonClientOnly();
    
    boolean portal_getAreAllWorldsLoaded();
    
    RegistryTracker.Modifiable portal_getDimensionTracker();
}
