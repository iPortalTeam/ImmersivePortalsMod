package qouteall.imm_ptl.core.mixin.common.registry;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Decoder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;
import java.util.Map;

@Mixin(RegistryDataLoader.class)
public class IERegistryDataLoader {
    @Invoker("createContext")
    public static RegistryOps.RegistryInfoLookup ip_createContext(RegistryAccess registryAccess, List<Pair<WritableRegistry<?>, RegistryDataLoader.Loader>> list) {
        throw new RuntimeException();
    }
    
    @Invoker("loadRegistryContents")
    public static <E> void ip_loadRegistryContents(RegistryOps.RegistryInfoLookup registryInfoLookup, ResourceManager resourceManager, ResourceKey<? extends Registry<E>> resourceKey, WritableRegistry<E> writableRegistry, Decoder<E> decoder, Map<ResourceKey<?>, Exception> map) {
        throw new RuntimeException();
    }
}
