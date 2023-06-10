package qouteall.q_misc_util.ducks;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelStorageSource;

import java.util.concurrent.Executor;

public interface IEMinecraftServer_Misc {
    
    LevelStorageSource.LevelStorageAccess ip_getStorageSource();
    
    Executor ip_getExecutor();
    
    void ip_addDimensionToWorldMap(ResourceKey<Level> dim, ServerLevel world);
    
    void ip_removeDimensionFromWorldMap(ResourceKey<Level> dimension);
    
    void ip_waitUntilNextTick();
    
    void ip_setStopped(boolean arg);
}
