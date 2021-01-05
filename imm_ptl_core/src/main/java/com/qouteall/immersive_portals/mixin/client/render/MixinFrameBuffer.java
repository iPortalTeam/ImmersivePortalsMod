package com.qouteall.immersive_portals.mixin.client.render;

import com.mojang.blaze3d.platform.FramebufferInfo;
import com.mojang.blaze3d.platform.GlStateManager;
import com.qouteall.immersive_portals.ducks.IEFrameBuffer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import org.lwjgl.opengl.ARBFramebufferObject;
import org.lwjgl.opengl.GL30;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.IntBuffer;

@Mixin(Framebuffer.class)
public abstract class MixinFrameBuffer implements IEFrameBuffer {
    
    private boolean isStencilBufferEnabled;
    
    @Shadow
    public int textureWidth;
    @Shadow
    public int textureHeight;
    
    @Shadow
    public int viewportWidth;
    
    @Shadow
    public int viewportHeight;
    
    @Shadow
    public int fbo;
    
    @Shadow
    public int colorAttachment;
    
    @Shadow
    public int depthAttachment;
    
    @Shadow
    @Final
    public boolean useDepthAttachment;
    
    @Shadow
    public abstract void setTexFilter(int i);
    
    @Shadow
    public abstract void checkFramebufferStatus();
    
    @Shadow
    public abstract void clear(boolean getError);
    
    @Shadow
    public abstract void endRead();
    
    @Shadow
    public abstract void initFbo(int width, int height, boolean getError);
    
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
    
    @Redirect(
        method = "initFbo",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/platform/GlStateManager;texImage2D(IIIIIIIILjava/nio/IntBuffer;)V"
        )
    )
    private void redirectTexImage2d(
        int target, int level, int internalFormat,
        int width, int height,
        int border, int format, int type,
        IntBuffer pixels
    ) {
        if (internalFormat == 6402 && isStencilBufferEnabled) {
            GlStateManager.texImage2D(
                target,
                level,
                ARBFramebufferObject.GL_DEPTH24_STENCIL8,
                width,
                height,
                border,
                ARBFramebufferObject.GL_DEPTH_STENCIL,
                GL30.GL_UNSIGNED_INT_24_8,
                pixels
            );
        }
        else {
            GlStateManager.texImage2D(
                target, level, internalFormat, width, height,
                border, format, type, pixels
            );
        }
    }
    
    @Redirect(
        method = "initFbo",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/platform/GlStateManager;framebufferTexture2D(IIIII)V"
        )
    )
    private void redirectFrameBufferTexture2d(
        int target, int attachment, int textureTarget, int texture, int level
    ) {
        if (attachment == FramebufferInfo.DEPTH_ATTACHMENT && isStencilBufferEnabled) {
            GlStateManager.framebufferTexture2D(
                target, GL30.GL_DEPTH_STENCIL_ATTACHMENT, textureTarget, texture, level
            );
        }
        else {
            GlStateManager.framebufferTexture2D(target, attachment, textureTarget, texture, level);
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
