package qouteall.imm_ptl.core.ducks;

import net.minecraft.util.FrameTimer;

public interface IEMinecraftServer {
    public FrameTimer getMetricsDataNonClientOnly();
    
    boolean portal_getAreAllWorldsLoaded();
}
