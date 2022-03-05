package qouteall.q_misc_util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.MappedRegistry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;

import java.util.Map;
import java.util.function.BiPredicate;

public class MiscHelper {
    
    public static <T> MappedRegistry<T> filterAndCopyRegistry(
        MappedRegistry<T> registry, BiPredicate<ResourceKey<T>, T> predicate
    ) {
        MappedRegistry<T> newRegistry = new MappedRegistry<>(
            registry.key(),
            registry.elementsLifecycle(),
            null
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
            runnable.run();
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
            runnable.run();
        }
        else {
            server.execute(runnable);
        }
    }
    
    
}
