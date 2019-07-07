package com.qouteall.immersive_portals.mixin;

import com.mojang.blaze3d.platform.GLX;
import net.minecraft.client.gl.GlFramebuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GlFramebuffer.class)
public class MixinGlFrameBuffer {
    @Inject(
        method = "Lnet/minecraft/client/gl/GlFramebuffer;initFbo(IIZ)V",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GLX;glBindRenderbuffer(II)V"),
        cancellable = true
    )
    private void onInitFrameBuffer(int int_1, int int_2, boolean boolean_1, CallbackInfo ci) {
        GlFramebuffer this_ = (GlFramebuffer) (Object) this;
        
        GLX.glRenderbufferStorage(
            GLX.GL_RENDERBUFFER,
            org.lwjgl.opengl.EXTPackedDepthStencil.GL_DEPTH24_STENCIL8_EXT,
            this_.texWidth,
            this_.texHeight
        );
        GLX.glFramebufferRenderbuffer(
            GLX.GL_FRAMEBUFFER,
            org.lwjgl.opengl.EXTFramebufferObject.GL_DEPTH_ATTACHMENT_EXT,
            GLX.GL_RENDERBUFFER,
            this_.depthAttachment
        );
        GLX.glFramebufferRenderbuffer(
            GLX.GL_FRAMEBUFFER,
            org.lwjgl.opengl.EXTFramebufferObject.GL_STENCIL_ATTACHMENT_EXT,
            GLX.GL_RENDERBUFFER,
            this_.depthAttachment
        );
        
        this_.checkFramebufferStatus();
        this_.clear(boolean_1);
        this_.endRead();
    }
}
