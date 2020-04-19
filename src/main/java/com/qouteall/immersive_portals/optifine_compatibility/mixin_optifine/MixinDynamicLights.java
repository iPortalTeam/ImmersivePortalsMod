package com.qouteall.immersive_portals.optifine_compatibility.mixin_optifine;

import com.qouteall.immersive_portals.CGlobal;
import net.minecraft.client.render.WorldRenderer;
import net.optifine.DynamicLights;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = DynamicLights.class, remap = false)
public class MixinDynamicLights {
    //avoid updating dynamic light when rendering portal
    @Inject(method = "update", at = @At("HEAD"), cancellable = true)
    private static void onUpdate(WorldRenderer renderGlobal, CallbackInfo ci) {
        if (CGlobal.renderer.isRendering()) {
            ci.cancel();
        }
    }
}
