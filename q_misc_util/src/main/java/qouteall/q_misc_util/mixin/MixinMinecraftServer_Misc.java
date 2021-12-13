package qouteall.q_misc_util.mixin;

import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.datafixers.DataFixer;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.ServerResourceManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldGenerationProgressListenerFactory;
import net.minecraft.util.UserCache;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.world.SaveProperties;
import net.minecraft.world.level.storage.LevelStorage;
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
    public abstract boolean isDedicated();
    
    @Inject(
        method = "<init>",
        at = @At("RETURN")
    )
    private void onConstruct(
        Thread serverThread, DynamicRegistryManager.Impl registryManager,
        LevelStorage.Session session, SaveProperties saveProperties,
        ResourcePackManager dataPackManager, Proxy proxy, DataFixer dataFixer,
        ServerResourceManager serverResourceManager, MinecraftSessionService sessionService,
        GameProfileRepository gameProfileRepo, UserCache userCache,
        WorldGenerationProgressListenerFactory worldGenerationProgressListenerFactory,
        CallbackInfo ci
    ) {
        MiscHelper.refMinecraftServer = new WeakReference<>((MinecraftServer) ((Object) this));
    }
}
