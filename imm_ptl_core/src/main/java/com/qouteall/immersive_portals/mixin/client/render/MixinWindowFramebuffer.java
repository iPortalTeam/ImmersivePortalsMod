package com.qouteall.immersive_portals.mixin.client.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.qouteall.immersive_portals.ducks.IEFrameBuffer;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.WindowFramebuffer;
import org.lwjgl.opengl.ARBFramebufferObject;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL30C;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.GL_DEPTH_COMPONENT;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;
import static org.lwjgl.opengl.GL30.GL_DEPTH_ATTACHMENT;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;

@Mixin(WindowFramebuffer.class)
public abstract class MixinWindowFramebuffer extends Framebuffer {
    
    public MixinWindowFramebuffer(boolean useDepth) {
        super(useDepth);
        throw new RuntimeException();
    }
    
    @Redirect(
        method = "supportsDepth",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/platform/GlStateManager;_texImage2D(IIIIIIIILjava/nio/IntBuffer;)V"
        )
    )
    private void onTexImage2D(
        int target, int level, int internalFormat,
        int width, int height, int border, int format, int type, IntBuffer pixels
    ) {
        boolean isStencilBufferEnabled = ((IEFrameBuffer) this).getIsStencilBufferEnabled();
        
        if (internalFormat == GL_DEPTH_COMPONENT && isStencilBufferEnabled) {
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
        method = "initSize",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/platform/GlStateManager;_glFramebufferTexture2D(IIIII)V"
        )
    )
    private void redirectFrameBufferTexture2d(
        int target, int attachment, int textureTarget, int texture, int level
    ) {
        boolean isStencilBufferEnabled = ((IEFrameBuffer) this).getIsStencilBufferEnabled();
        
        if (attachment == GL30C.GL_DEPTH_ATTACHMENT && isStencilBufferEnabled) {
            GlStateManager._glFramebufferTexture2D(
                target, GL30.GL_DEPTH_STENCIL_ATTACHMENT, textureTarget, texture, level
            );
        }
        else {
            GlStateManager._glFramebufferTexture2D(target, attachment, textureTarget, texture, level);
        }
    }

//    /**
//     * @author
//     */
//    @Overwrite
//    private void initSize(int width, int height) {
//        RenderSystem.assertThread(RenderSystem::isOnRenderThreadOrInit);
////        WindowFramebuffer.Size size = this.findSuitableSize(width, height);
//        this.fbo = GlStateManager.glGenFramebuffers();
//        GlStateManager._glBindFramebuffer(GL_FRAMEBUFFER, this.fbo);
//        GlStateManager._bindTexture(this.colorAttachment);
//        GlStateManager._texParameter(3553, 10241, 9728);
//        GlStateManager._texParameter(3553, 10240, 9728);
//        GlStateManager._texParameter(3553, 10242, 33071);
//        GlStateManager._texParameter(3553, 10243, 33071);
//        GlStateManager._glFramebufferTexture2D(
//            GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0,
//            GL_TEXTURE_2D, this.colorAttachment, 0
//        );
//        GlStateManager._bindTexture(this.depthAttachment);
//        GlStateManager._texParameter(3553, 34892, 0);
//        GlStateManager._texParameter(3553, 10241, 9728);
//        GlStateManager._texParameter(3553, 10240, 9728);
//        GlStateManager._texParameter(3553, 10242, 33071);
//        GlStateManager._texParameter(3553, 10243, 33071);
//        GlStateManager._glFramebufferTexture2D(
//            GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT,
//            GL_TEXTURE_2D, this.depthAttachment, 0
//        );
//        GlStateManager._bindTexture(0);
////        this.viewportWidth = size.width;
////        this.viewportHeight = size.height;
////        this.textureWidth = size.width;
////        this.textureHeight = size.height;
//        this.checkFramebufferStatus();
//        GlStateManager._glBindFramebuffer(GL_FRAMEBUFFER, 0);
//    }
}
