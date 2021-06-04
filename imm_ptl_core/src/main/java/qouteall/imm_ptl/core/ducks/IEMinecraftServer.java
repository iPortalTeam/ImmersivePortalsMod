package qouteall.imm_ptl.core.ducks;

import net.minecraft.util.MetricsData;

public interface IEMinecraftServer {
    public MetricsData getMetricsDataNonClientOnly();
    
    boolean portal_getAreAllWorldsLoaded();
}
