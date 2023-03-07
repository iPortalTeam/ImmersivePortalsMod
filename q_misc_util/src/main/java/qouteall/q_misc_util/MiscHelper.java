package qouteall.q_misc_util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.MappedRegistry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qouteall.q_misc_util.ducks.IEMinecraftServer_Misc;
import qouteall.q_misc_util.mixin.IELevelStorageAccess_Misc;

import java.nio.file.Path;
import java.util.Map;
import java.util.function.BiPredicate;

public class MiscHelper {
    
    private static final Logger LOGGER = LogManager.getLogger();
    
    public static <T> MappedRegistry<T> filterAndCopyRegistry(
        MappedRegistry<T> registry, BiPredicate<ResourceKey<T>, T> predicate
    ) {
        MappedRegistry<T> newRegistry = new MappedRegistry<>(
            registry.key(),
            registry.registryLifecycle()
        );
        
        for (Map.Entry<ResourceKey<T>, T> entry : registry.entrySet()) {
            T object = entry.getValue();
            ResourceKey<T> key = entry.getKey();
            if (predicate.test(key, object)) {
                newRegistry.register(
                    key, object, registry.lifecycle(object)
                );
            }
        }
        
        return newRegistry;
    }
    
    /**
     * {@link ReentrantThreadExecutor#shouldExecuteAsync()}
     * The execution may get deferred on the render thread
     */
    @Environment(EnvType.CLIENT)
    public static void executeOnRenderThread(Runnable runnable) {
        Minecraft client = Minecraft.getInstance();
        
        if (client.isSameThread()) {
            try {
                runnable.run();
            }
            catch (Exception e) {
                LOGGER.error("Processing task on render thread", e);
            }
        }
        else {
            client.execute(runnable);
        }
    }
    
    public static MinecraftServer getServer() {
        return MiscGlobals.refMinecraftServer.get();
    }
    
    public static void executeOnServerThread(Runnable runnable) {
        MinecraftServer server = getServer();
        
        if (server.isSameThread()) {
            try {
                runnable.run();
            }
            catch (Exception e) {
                LOGGER.error("Processing task on server thread", e);
            }
        }
        else {
            server.execute(runnable);
        }
    }
    
    public static boolean isDedicatedServer() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER;
    }
    
    
    public static Path getWorldSavingDirectory() {
        MinecraftServer server = getServer();
        Validate.notNull(server);
        Path saveDir =
            ((IELevelStorageAccess_Misc) ((IEMinecraftServer_Misc) server).ip_getStorageSource())
                .ip_getLevelPath().path();
        return saveDir;
    }
}
