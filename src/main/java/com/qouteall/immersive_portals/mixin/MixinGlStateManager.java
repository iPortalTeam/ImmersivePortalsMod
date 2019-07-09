package com.qouteall.immersive_portals.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import com.qouteall.immersive_portals.Globals;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GlStateManager.class)
public class MixinGlStateManager {
//    @Inject(
//        method = "Lcom/mojang/blaze3d/platform/GlStateManager;clear(IZ)V",
//        at = @At("HEAD"),
//        cancellable = true
//    )
//    private static void onClear(int int_1, boolean boolean_1, CallbackInfo ci) {
//        if (Globals.portalRenderManager.shouldSkipClearing()) {
//            ci.cancel();
//        }
//    }
}
