package com.qouteall.immersive_portals.mixin_client;

import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.platform.GlStateManager;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.exposer.IEGlFrameBuffer;
import com.qouteall.immersive_portals.my_util.Helper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GlFramebuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GlFramebuffer.class)
public abstract class MixinGlFrameBuffer implements IEGlFrameBuffer {
    
    private boolean isStencilBufferEnabled;
    
    @Shadow
    public int texWidth;
    @Shadow
    public int texHeight;
    
    @Shadow
    public abstract void initFbo(int int_1, int int_2, boolean boolean_1);
    
    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(
        int int_1,
        int int_2,
        boolean boolean_1,
        boolean boolean_2,
        CallbackInfo ci
    ) {
        isStencilBufferEnabled = false;
    }
    
    @Inject(
        method = "Lnet/minecraft/client/gl/GlFramebuffer;initFbo(IIZ)V",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GLX;glBindRenderbuffer(II)V"),
        cancellable = true
    )
    private void onInitFrameBuffer(int int_1, int int_2, boolean isMac, CallbackInfo ci) {
        if (isStencilBufferEnabled) {
            GlFramebuffer this_ = (GlFramebuffer) (Object) this;
        
            GLX.glBindRenderbuffer(GLX.GL_RENDERBUFFER, this_.depthAttachment);
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
            this_.clear(isMac);
            this_.endRead();
        
            Helper.checkGlError();
        
            Helper.log("Frame Buffer Reloaded with Stencil Buffer");
        
            ci.cancel();
        }
    }
    
    @Redirect(
        method = "draw(IIZ)V",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/platform/GlStateManager;disableAlphaTest()V"
        )
    )
    private void redirectDisableAlphaTest() {
        if (CGlobal.doDisableAlphaTestWhenRenderingFrameBuffer) {
            GlStateManager.disableAlphaTest();
        }
    }
    
    @Override
    public boolean getIsStencilBufferEnabled() {
        return isStencilBufferEnabled;
    }
    
    @Override
    public void setIsStencilBufferEnabledAndReload(boolean cond) {
        if (isStencilBufferEnabled != cond) {
            isStencilBufferEnabled = cond;
            initFbo(texWidth, texHeight, MinecraftClient.IS_SYSTEM_MAC);
        }
    }
}
