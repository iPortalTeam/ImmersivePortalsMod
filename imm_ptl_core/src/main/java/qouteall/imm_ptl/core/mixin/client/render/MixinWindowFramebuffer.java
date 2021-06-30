package qouteall.imm_ptl.core.mixin.client.render;

import com.mojang.blaze3d.platform.GlStateManager;
import qouteall.imm_ptl.core.IPCGlobal;
import qouteall.imm_ptl.core.ducks.IEFrameBuffer;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.WindowFramebuffer;
import org.lwjgl.opengl.ARBFramebufferObject;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL30C;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.GL_DEPTH_COMPONENT;
import static org.lwjgl.opengl.GL30.GL_DEPTH24_STENCIL8;
import static org.lwjgl.opengl.GL30.GL_DEPTH32F_STENCIL8;
import static org.lwjgl.opengl.GL30.GL_FLOAT_32_UNSIGNED_INT_24_8_REV;

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
                IPCGlobal.useAnotherStencilFormat ? GL_DEPTH32F_STENCIL8 : GL_DEPTH24_STENCIL8,//
                width,
                height,
                border,
                ARBFramebufferObject.GL_DEPTH_STENCIL,
                IPCGlobal.useAnotherStencilFormat ? GL_FLOAT_32_UNSIGNED_INT_24_8_REV : GL30.GL_UNSIGNED_INT_24_8,//
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
    
}
