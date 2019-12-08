package com.qouteall.immersive_portals.mixin_client;

import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.ducks.IEGlFrameBuffer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import org.lwjgl.opengl.GL30;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Framebuffer.class)
public abstract class MixinGlFrameBuffer implements IEGlFrameBuffer {
    
    private boolean isStencilBufferEnabled;
    
    @Shadow
    public int textureWidth;
    @Shadow
    public int textureHeight;
    
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
        method = "initFbo",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/platform/GlStateManager;bindRenderbuffer(II)V"
        ),
        cancellable = true
    )
    private void onInitFrameBuffer(int int_1, int int_2, boolean isMac, CallbackInfo ci) {
        if (isStencilBufferEnabled) {
            Framebuffer this_ = (Framebuffer) (Object) this;
    
            GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, this_.depthAttachment);
            GL30.glRenderbufferStorage(
                GL30.GL_RENDERBUFFER,
                org.lwjgl.opengl.EXTPackedDepthStencil.GL_DEPTH24_STENCIL8_EXT,
                this_.textureWidth,
                this_.textureHeight
            );
            GL30.glFramebufferRenderbuffer(
                GL30.GL_FRAMEBUFFER,
                org.lwjgl.opengl.EXTFramebufferObject.GL_DEPTH_ATTACHMENT_EXT,
                GL30.GL_RENDERBUFFER,
                this_.depthAttachment
            );
            GL30.glFramebufferRenderbuffer(
                GL30.GL_FRAMEBUFFER,
                org.lwjgl.opengl.EXTFramebufferObject.GL_STENCIL_ATTACHMENT_EXT,
                GL30.GL_RENDERBUFFER,
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
    
    @Override
    public boolean getIsStencilBufferEnabled() {
        return isStencilBufferEnabled;
    }
    
    @Override
    public void setIsStencilBufferEnabledAndReload(boolean cond) {
        if (isStencilBufferEnabled != cond) {
            isStencilBufferEnabled = cond;
            initFbo(textureWidth, textureHeight, MinecraftClient.IS_SYSTEM_MAC);
        }
    }
}
