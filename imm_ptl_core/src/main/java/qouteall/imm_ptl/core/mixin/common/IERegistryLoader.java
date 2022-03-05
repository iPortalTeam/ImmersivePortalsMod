package qouteall.imm_ptl.core.mixin.common;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.RegistryLoader;
import net.minecraft.resources.RegistryResourceAccess;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(RegistryLoader.class)
public interface IERegistryLoader {
    @Invoker("<init>")
    public static RegistryLoader construct(RegistryResourceAccess registryResourceAccess) {
        throw new RuntimeException();
    }
    
    @Invoker("overrideRegistryFromResources")
    public <E> DataResult<Registry<E>> ip_overrideRegistryFromResources(
        WritableRegistry<E> writableRegistry, ResourceKey<? extends Registry<E>> resourceKey,
        Codec<E> codec, DynamicOps<JsonElement> dynamicOps
    );
}
