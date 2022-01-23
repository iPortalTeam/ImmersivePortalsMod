package qouteall.imm_ptl.core.ducks;

import net.minecraft.util.FrameTimer;
import net.minecraft.world.level.storage.LevelStorageSource;

public interface IEMinecraftServer {
    public FrameTimer getMetricsDataNonClientOnly();
    
    boolean portal_getAreAllWorldsLoaded();
}
