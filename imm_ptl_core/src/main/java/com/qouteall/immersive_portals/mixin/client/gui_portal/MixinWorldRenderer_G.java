package com.qouteall.immersive_portals.mixin.client.gui_portal;

import com.qouteall.immersive_portals.render.GuiPortalRendering;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldRenderer.class)
public class MixinWorldRenderer_G {
    @Inject(method = "canDrawEntityOutlines", at = @At("HEAD"), cancellable = true)
    private void onCanDrawEntityOutlines(CallbackInfoReturnable<Boolean> cir) {
        if (GuiPortalRendering.isRendering()) {
            cir.setReturnValue(false);
        }
    }
}
