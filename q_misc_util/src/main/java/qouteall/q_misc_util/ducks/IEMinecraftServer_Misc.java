package qouteall.q_misc_util.ducks;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.concurrent.Executor;

public interface IEMinecraftServer_Misc {
    
    LevelStorageSource.LevelStorageAccess ip_getStorageSource();
    
    Executor ip_getExecutor();
    
    void addDimensionToWorldMap(ResourceKey<Level> dim, ServerLevel world);
    
}
