package qouteall.q_misc_util.mixin.dimension;

import com.mojang.serialization.Lifecycle;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import qouteall.q_misc_util.LifecycleHack;

@Mixin(WorldDimensions.class)
public class MixinWorldDimensions {
    // hack lifecycle
    @Inject(
        method = "isVanillaLike", at = @At("RETURN"), cancellable = true
    )
    private static void onIsVanillaLike(
        ResourceKey<LevelStem> resourceKey, LevelStem levelStem, CallbackInfoReturnable<Boolean> cir
    ) {
        String namespace = resourceKey.location().getNamespace();
        if (LifecycleHack.isNamespaceStable(namespace)) {
            cir.setReturnValue(true);
        }
    }
    
    // hack lifecycle
    @Redirect(
        method = "bake",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/serialization/Lifecycle;experimental()Lcom/mojang/serialization/Lifecycle;",
            remap = false
        )
    )
    private Lifecycle redirectLifecycle() {
        return Lifecycle.stable();
    }
}
