package qouteall.q_misc_util.mixin;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MinecraftServer.class)
public interface IEMinecraftServer_Misc {
    @Accessor("storageSource")
    LevelStorageSource.LevelStorageAccess ip_getStorageSource();
}
