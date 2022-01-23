package qouteall.q_misc_util.mixin;

import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.datafixers.DataFixer;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerResources;
import net.minecraft.server.level.progress.ChunkProgressListenerFactory;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.WorldData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.q_misc_util.MiscHelper;

import java.lang.ref.WeakReference;
import java.net.Proxy;

@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer_Misc {
    @Shadow
    public abstract boolean isDedicatedServer();
    
    @Inject(
        method = "<init>",
        at = @At("RETURN")
    )
    private void onConstruct(
        Thread serverThread, RegistryAccess.RegistryHolder registryManager,
        LevelStorageSource.LevelStorageAccess session, WorldData saveProperties,
        PackRepository dataPackManager, Proxy proxy, DataFixer dataFixer,
        ServerResources serverResourceManager, MinecraftSessionService sessionService,
        GameProfileRepository gameProfileRepo, GameProfileCache userCache,
        ChunkProgressListenerFactory worldGenerationProgressListenerFactory,
        CallbackInfo ci
    ) {
        MiscHelper.refMinecraftServer = new WeakReference<>((MinecraftServer) ((Object) this));
    }
}
