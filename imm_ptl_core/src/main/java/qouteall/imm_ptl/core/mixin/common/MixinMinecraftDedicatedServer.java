package qouteall.imm_ptl.core.mixin.common;

import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.datafixers.DataFixer;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.ServerResourceManager;
import net.minecraft.server.WorldGenerationProgressListenerFactory;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.dedicated.MinecraftDedicatedServer;
import net.minecraft.server.dedicated.ServerPropertiesLoader;
import net.minecraft.util.UserCache;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.world.SaveProperties;
import net.minecraft.world.level.storage.LevelStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.platform_specific.O_O;

@Mixin(MinecraftDedicatedServer.class)
public class MixinMinecraftDedicatedServer {
    @Inject(
        method = "<init>", at = @At("RETURN")
    )
    private void onInitEnded(
        Thread serverThread, DynamicRegistryManager.Impl registryManager,
        LevelStorage.Session session, ResourcePackManager dataPackManager,
        ServerResourceManager serverResourceManager, SaveProperties saveProperties,
        ServerPropertiesLoader propertiesLoader, DataFixer dataFixer,
        MinecraftSessionService sessionService, GameProfileRepository gameProfileRepo,
        UserCache userCache,
        WorldGenerationProgressListenerFactory worldGenerationProgressListenerFactory,
        CallbackInfo ci
    ) {
        // loading it requires getting the server directory
        O_O.loadConfigFabric();
    }
}
