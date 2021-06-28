package qouteall.imm_ptl.core;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;

import java.nio.IntBuffer;

import static org.lwjgl.opengl.ARBShadow.GL_TEXTURE_COMPARE_MODE_ARB;
import static org.lwjgl.opengl.GL11.GL_DEPTH_COMPONENT;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_RGBA8;
import static org.lwjgl.opengl.GL11.GL_STENCIL;
import static org.lwjgl.opengl.GL11.GL_STENCIL_INDEX;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL14.GL_TEXTURE_COMPARE_MODE;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;
import static org.lwjgl.opengl.GL30.GL_DEPTH24_STENCIL8;
import static org.lwjgl.opengl.GL30.GL_DEPTH_ATTACHMENT;
import static org.lwjgl.opengl.GL30.GL_DEPTH_STENCIL;
import static org.lwjgl.opengl.GL30.GL_DEPTH_STENCIL_ATTACHMENT;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.GL_STENCIL_ATTACHMENT;
import static org.lwjgl.opengl.GL30.GL_STENCIL_INDEX8;
import static org.lwjgl.opengl.GL30.GL_UNSIGNED_INT_24_8;

public class FramebufferWithSeparateStencil {
    public int textureWidth;
    public int textureHeight;
    public int viewportWidth;
    public int viewportHeight;
    public int fbo;
    public int colorAttachment;
    public int depthAttachment;
    public int stencilAttachment;
    
    public FramebufferWithSeparateStencil() {
    
    }
    
    public void initFbo(int width, int height, boolean getError) {
        RenderSystem.assertThread(RenderSystem::isOnRenderThreadOrInit);
        int maxTexSize = RenderSystem.maxSupportedTextureSize();
        
        this.viewportWidth = width;
        this.viewportHeight = height;
        this.textureWidth = width;
        this.textureHeight = height;
        this.fbo = GlStateManager.glGenFramebuffers();
        this.colorAttachment = TextureUtil.generateTextureId();
        this.depthAttachment = TextureUtil.generateTextureId();
        this.stencilAttachment = TextureUtil.generateTextureId();
        
        GlStateManager._bindTexture(this.depthAttachment);
        
        GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_MODE_ARB, 0);
        GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        GlStateManager._texImage2D(
            GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT,
            this.textureWidth, this.textureHeight, 0, GL_DEPTH_COMPONENT,
            GL_FLOAT, (IntBuffer) null
        );
        
        
        GlStateManager._bindTexture(this.colorAttachment);
        GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        GlStateManager._texImage2D(
            GL_TEXTURE_2D, 0, GL_RGBA8, this.textureWidth, this.textureHeight,
            0, GL_RGBA, GL_UNSIGNED_BYTE, (IntBuffer) null
        );
        
        GlStateManager._bindTexture(stencilAttachment);
//        GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
//        GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        GlStateManager._texImage2D(
            GL_TEXTURE_2D, 0, GL_STENCIL_INDEX8,//   GL_DEPTH_STENCIL
            this.textureWidth, this.textureHeight, 0,
            GL_STENCIL_INDEX, GL_UNSIGNED_BYTE, null
        );
        
        CHelper.checkGlError();
        
        GlStateManager._glBindFramebuffer(GL_FRAMEBUFFER, this.fbo);
        GlStateManager._glFramebufferTexture2D(
            GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, this.colorAttachment, 0
        );
        GlStateManager._glFramebufferTexture2D(
            GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, this.depthAttachment, 0
        );
        GlStateManager._glFramebufferTexture2D(
            GL_FRAMEBUFFER, GL_STENCIL_ATTACHMENT, GL_TEXTURE_2D, this.stencilAttachment, 0
        );
        
        this.checkFramebufferStatus();
        
    }
    
    public void checkFramebufferStatus() {
        RenderSystem.assertThread(RenderSystem::isOnRenderThreadOrInit);
        int i = GlStateManager.glCheckFramebufferStatus(36160);
        if (i != 36053) {
            if (i == 36054) {
                throw new RuntimeException("GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT");
            }
            else if (i == 36055) {
                throw new RuntimeException("GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT");
            }
            else if (i == 36059) {
                throw new RuntimeException("GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER");
            }
            else if (i == 36060) {
                throw new RuntimeException("GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER");
            }
            else if (i == 36061) {
                throw new RuntimeException("GL_FRAMEBUFFER_UNSUPPORTED");
            }
            else if (i == 1285) {
                throw new RuntimeException("GL_OUT_OF_MEMORY");
            }
            else {
                throw new RuntimeException("glCheckFramebufferStatus returned unknown status:" + i);
            }
        }
    }
}
