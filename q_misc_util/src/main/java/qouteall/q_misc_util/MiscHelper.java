package qouteall.q_misc_util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.util.registry.SimpleRegistry;
import net.minecraft.util.thread.ReentrantThreadExecutor;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.function.BiPredicate;

public class MiscHelper {
    public static WeakReference<MinecraftServer> refMinecraftServer =
        new WeakReference<>(null);
    
    public static <T> SimpleRegistry<T> filterAndCopyRegistry(
        SimpleRegistry<T> registry, BiPredicate<RegistryKey<T>, T> predicate
    ) {
        SimpleRegistry<T> newRegistry = new SimpleRegistry<>(
            registry.getKey(),
            registry.getLifecycle()
        );
        
        for (Map.Entry<RegistryKey<T>, T> entry : registry.getEntries()) {
            T object = entry.getValue();
            RegistryKey<T> key = entry.getKey();
            if (predicate.test(key, object)) {
                newRegistry.add(
                    key, object, registry.getEntryLifecycle(object)
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
        MinecraftClient client = MinecraftClient.getInstance();
        
        if (client.isOnThread()) {
            runnable.run();
        }
        else {
            client.execute(runnable);
        }
    }
    
    public static MinecraftServer getServer() {
        return refMinecraftServer.get();
    }
    
    public static void executeOnServerThread(Runnable runnable) {
        MinecraftServer server = getServer();
        
        if (server.isOnThread()) {
            runnable.run();
        }
        else {
            server.execute(runnable);
        }
    }
}
