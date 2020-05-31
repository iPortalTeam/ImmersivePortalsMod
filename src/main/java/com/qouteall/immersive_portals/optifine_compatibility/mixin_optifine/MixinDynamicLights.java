package com.qouteall.immersive_portals.optifine_compatibility.mixin_optifine;

import com.qouteall.immersive_portals.render.context_management.RenderStates;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.optifine.DynamicLights", remap = false)
public class MixinDynamicLights {
    //avoid updating dynamic light when rendering portal
    @Inject(method = "update", at = @At("HEAD"), cancellable = true)
    private static void onUpdate(WorldRenderer renderGlobal, CallbackInfo ci) {
        if (RenderStates.isRendering()) {
            ci.cancel();
        }
    }
    
//    @ModifyConstant(
//        method = "Lnet/optifine/DynamicLights;getLightLevel(Lnet/minecraft/util/math/BlockPos;)D",
//        constant = @Constant(doubleValue = 56.25D)
//    )
//    private static double modifyMaxDist(double original) {
//        return 256.0d;
//    }
//
//    @ModifyConstant(
//        method = "Lnet/optifine/DynamicLights;getLightLevel(Lnet/minecraft/util/math/BlockPos;)D",
//        constant = @Constant(doubleValue = 7.5D)
//    )
//    private static double modifyRatio(double original) {
//        return 16.0d;
//    }
}
