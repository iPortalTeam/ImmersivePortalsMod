package qouteall.imm_ptl.core.mixin.client.render.framebuffer;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.Minecraft;
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
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.IPCGlobal;
import qouteall.imm_ptl.core.ducks.IEFrameBuffer;

import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.GL_DEPTH_COMPONENT;
import static org.lwjgl.opengl.GL30.GL_DEPTH24_STENCIL8;
import static org.lwjgl.opengl.GL30.GL_DEPTH32F_STENCIL8;
import static org.lwjgl.opengl.GL30.GL_FLOAT_32_UNSIGNED_INT_24_8_REV;

@Mixin(RenderTarget.class)
public abstract class MixinRenderTarget implements IEFrameBuffer {
    
    private boolean isStencilBufferEnabled;
    
    @Shadow
    public int width;
    @Shadow
    public int height;
    
    @Shadow
    public int viewWidth;
    
    @Shadow
    public int viewHeight;
    
    @Shadow
    public int frameBufferId;
    
    @Shadow
    public int colorTextureId;
    
    @Shadow
    public int depthBufferId;
    
    @Shadow
    @Final
    public boolean useDepth;
    
    @Shadow
    public abstract void setFilterMode(int i);
    
    @Shadow
    public abstract void checkStatus();
    
    @Shadow
    public abstract void clear(boolean getError);
    
    @Shadow
    public abstract void unbindRead();
    
    @Shadow public abstract void createBuffers(int width, int height, boolean getError);
    
    @Shadow public abstract void resize(int width, int height, boolean clearError);
    
    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(
        boolean useDepth,
        CallbackInfo ci
    ) {
        isStencilBufferEnabled = false;
    }
    
    //6402 0x1902 GL_DEPTH_COMPONENT
    //36096 0x8D00 GL_DEPTH_ATTACHMENT
    
    // https://github.com/Astrarre/Astrarre/blob/1.17/astrarre-rendering-v0/src/main/java/io/github/astrarre/rendering/internal/mixin/FramebufferMixin_EnableStencil.java
    
//    /**
//     * @author
//     */
//    @Overwrite
//    public void initFbo(int width, int height, boolean getError) {
//        RenderSystem.assertThread(RenderSystem::isOnRenderThreadOrInit);
//        int maxTexSize = RenderSystem.maxSupportedTextureSize();
//        if (width > 0 && width <= maxTexSize && height > 0 && height <= maxTexSize) {
//            this.viewportWidth = width;
//            this.viewportHeight = height;
//            this.textureWidth = width;
//            this.textureHeight = height;
//            this.fbo = GlStateManager.glGenFramebuffers();
//            this.colorAttachment = TextureUtil.generateTextureId();
//            if (this.useDepthAttachment) {
//                this.depthAttachment = TextureUtil.generateTextureId();
//                GlStateManager._bindTexture(this.depthAttachment);
//
//                GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
//                GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
//                GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_MODE_ARB, 0);
//                GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
//                GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
//                GlStateManager._texImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT, this.textureWidth, this.textureHeight, 0, GL_DEPTH_COMPONENT, GL_FLOAT, (IntBuffer)null);
//            }
//
//            this.setTexFilter(GL_NEAREST);
//            GlStateManager._bindTexture(this.colorAttachment);
//            GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
//            GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
//            GlStateManager._texImage2D(
//                GL_TEXTURE_2D, 0, GL_RGBA8, this.textureWidth, this.textureHeight,
//                0, GL_RGBA, GL_UNSIGNED_BYTE, (IntBuffer)null
//            );
//            GlStateManager._glBindFramebuffer(GL_FRAMEBUFFER, this.fbo);
//            GlStateManager._glFramebufferTexture2D(
//                GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, this.colorAttachment, 0
//            );
//            if (this.useDepthAttachment) {
//                GlStateManager._glFramebufferTexture2D(
//                    GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, this.depthAttachment, 0
//                );
//            }
//
//            this.checkFramebufferStatus();
//            this.clear(getError);
//            this.endRead();
//        } else {
//            throw new IllegalArgumentException("Window " + width + "x" + height + " size out of bounds (max. size: " + maxTexSize + ")");
//        }
//    }
    

    @Redirect(
        method = "Lcom/mojang/blaze3d/pipeline/RenderTarget;createBuffers(IIZ)V",
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
        if (internalFormat == GL_DEPTH_COMPONENT && isStencilBufferEnabled) {
            GlStateManager._texImage2D(
                target,
                level,
                IPCGlobal.useAnotherStencilFormat ? GL_DEPTH32F_STENCIL8 : GL_DEPTH24_STENCIL8,
                width,
                height,
                border,
                ARBFramebufferObject.GL_DEPTH_STENCIL,
                IPCGlobal.useAnotherStencilFormat ? GL_FLOAT_32_UNSIGNED_INT_24_8_REV : GL30.GL_UNSIGNED_INT_24_8,
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
        method = "Lcom/mojang/blaze3d/pipeline/RenderTarget;createBuffers(IIZ)V",
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
    
    @Inject(
        method = "Lcom/mojang/blaze3d/pipeline/RenderTarget;copyDepthFrom(Lcom/mojang/blaze3d/pipeline/RenderTarget;)V",
        at = @At("RETURN")
    )
    private void onCopiedDepthFrom(RenderTarget framebuffer, CallbackInfo ci) {
        CHelper.checkGlError();
    }
    
    @Override
    public boolean getIsStencilBufferEnabled() {
        return isStencilBufferEnabled;
    }
    
    @Override
    public void setIsStencilBufferEnabledAndReload(boolean cond) {
        if (isStencilBufferEnabled != cond) {
            isStencilBufferEnabled = cond;
            resize(width, height, Minecraft.ON_OSX);
        }
    }
}
