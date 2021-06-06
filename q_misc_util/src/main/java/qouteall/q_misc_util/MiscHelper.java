package qouteall.q_misc_util;

import net.minecraft.util.registry.RegistryKey;
import net.minecraft.util.registry.SimpleRegistry;

import java.util.Map;
import java.util.function.BiPredicate;

public class MiscHelper {
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
}
