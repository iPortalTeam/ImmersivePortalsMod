package qouteall.q_misc_util.mixin;

import com.mojang.serialization.Lifecycle;
import net.minecraft.core.MappedRegistry;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import qouteall.q_misc_util.MiscGlobals;

@Mixin(MappedRegistry.class)
public class MixinMappedRegistry<T> {
//    @ModifyVariable(
//        method = "registerMapping(ILnet/minecraft/resources/ResourceKey;Ljava/lang/Object;Lcom/mojang/serialization/Lifecycle;Z)Lnet/minecraft/core/Holder;",
//        at = @At("HEAD"),
//        argsOnly = true,
//        index = 4
//    )
//    private Lifecycle modifyLifecycle(
//        Lifecycle value,
//        int i, ResourceKey<T> resourceKey, T object, Lifecycle lifecycle, boolean bl
//    ) {
//        if (MiscGlobals.stableNamespaces.contains(resourceKey.location().getNamespace())) {
//            return Lifecycle.stable();
//        }
//        else {
//            return lifecycle;
//        }
//    }
}
