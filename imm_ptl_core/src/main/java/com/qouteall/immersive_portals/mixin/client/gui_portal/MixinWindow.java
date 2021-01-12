package com.qouteall.immersive_portals.mixin.client.gui_portal;

import com.qouteall.immersive_portals.render.GuiPortalRendering;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Window.class)
public class MixinWindow {
    @Inject(method = "getFramebufferWidth", at = @At("HEAD"), cancellable = true)
    private void onGetFramebufferWidth(CallbackInfoReturnable<Integer> cir) {
        Framebuffer guiPortalRenderingFb = GuiPortalRendering.getRenderingFrameBuffer();
        if (guiPortalRenderingFb != null) {
            cir.setReturnValue(guiPortalRenderingFb.viewportWidth);
        }
    }
    
    @Inject(method = "getFramebufferHeight", at = @At("HEAD"), cancellable = true)
    private void onGetFramebufferHeight(CallbackInfoReturnable<Integer> cir) {
        Framebuffer guiPortalRenderingFb = GuiPortalRendering.getRenderingFrameBuffer();
        if (guiPortalRenderingFb != null) {
            cir.setReturnValue(guiPortalRenderingFb.viewportHeight);
        }
    }
}
