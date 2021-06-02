package com.qouteall.immersive_portals.mixin.client.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.qouteall.immersive_portals.ducks.IEFrameBuffer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import org.lwjgl.opengl.ARBFramebufferObject;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL30C;
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
        boolean useDepth,
        CallbackInfo ci
    ) {
        isStencilBufferEnabled = false;
    }
    
    @Redirect(
        method = "initFbo",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/platform/GlStateManager;_texImage2D(IIIIIIIILjava/nio/IntBuffer;)V"
        )
    )
    private void redirectTexImage2d(
        int target, int level, int internalFormat,
        int width, int height,
        int border, int format, int type,
        IntBuffer pixels
    ) {
        if (internalFormat == 6402 && isStencilBufferEnabled) {
            GlStateManager._texImage2D(
                target,
                level,
                ARBFramebufferObject.GL_DEPTH24_STENCIL8,//GL_DEPTH32F_STENCIL8
                width,
                height,
                border,
                ARBFramebufferObject.GL_DEPTH_STENCIL,
                GL30.GL_UNSIGNED_INT_24_8,//GL_FLOAT_32_UNSIGNED_INT_24_8_REV
                pixels
            );
        }
        else {
            GlStateManager._texImage2D(
                target, level, internalFormat, width, height,
                border, format, type, pixels
            );
        }
    }
    
    @Redirect(
        method = "initFbo",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/platform/GlStateManager;_glFramebufferTexture2D(IIIII)V"
        )
    )
    private void redirectFrameBufferTexture2d(
        int target, int attachment, int textureTarget, int texture, int level
    ) {
        
        if (attachment == GL30C.GL_DEPTH_ATTACHMENT && isStencilBufferEnabled) {
            GlStateManager._glFramebufferTexture2D(
                target, GL30.GL_DEPTH_STENCIL_ATTACHMENT, textureTarget, texture, level
            );
        }
        else {
            GlStateManager._glFramebufferTexture2D(target, attachment, textureTarget, texture, level);
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
