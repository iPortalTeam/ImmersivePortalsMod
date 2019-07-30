package com.qouteall.immersive_portals.mixin_client;

import com.mojang.blaze3d.platform.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;

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
