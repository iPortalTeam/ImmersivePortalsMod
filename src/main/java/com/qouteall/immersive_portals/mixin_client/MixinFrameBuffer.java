package com.qouteall.immersive_portals.mixin_client;

import com.mojang.blaze3d.platform.FramebufferInfo;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.qouteall.immersive_portals.CHelper;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.ducks.IEFrameBuffer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.texture.TextureUtil;
import org.lwjgl.opengl.ARBFramebufferObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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
    
    /**
     * @author qouteall
     */
    @Overwrite
    public void initFbo(int width, int height, boolean getError) {
        RenderSystem.assertThread(RenderSystem::isOnRenderThreadOrInit);
        this.viewportWidth = width;
        this.viewportHeight = height;
        this.textureWidth = width;
        this.textureHeight = height;
        this.fbo = GlStateManager.genFramebuffers();
        this.colorAttachment = TextureUtil.generateId();
        if (this.useDepthAttachment) {
            this.depthAttachment = TextureUtil.generateId();
            GlStateManager.bindTexture(this.depthAttachment);
            GlStateManager.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, 9728);
            GlStateManager.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, 9728);
            GlStateManager.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, 10496);
            GlStateManager.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, 10496);
            GlStateManager.texParameter(GL11.GL_TEXTURE_2D, GL14.GL_TEXTURE_COMPARE_MODE, 0);
            if (isStencilBufferEnabled) {
                //https://www.khronos.org/registry/OpenGL-Refpages/es3.0/html/glTexImage2D.xhtml
                GlStateManager.texImage2D(
                    GL11.GL_TEXTURE_2D, 0, ARBFramebufferObject.GL_DEPTH24_STENCIL8,
                    this.textureWidth, this.textureHeight,
                    0, ARBFramebufferObject.GL_DEPTH_STENCIL,
                    GL30.GL_UNSIGNED_INT_24_8, (IntBuffer) null
                );
            }
            else {
                GlStateManager.texImage2D(
                    GL11.GL_TEXTURE_2D, 0, GL11.GL_DEPTH_COMPONENT,
                    this.textureWidth, this.textureHeight,
                    0, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, (IntBuffer) null
                );
            }
        }
        
        this.setTexFilter(9728);
        GlStateManager.bindTexture(this.colorAttachment);
        GlStateManager.texImage2D(
            GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8,
            this.textureWidth, this.textureHeight,
            0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (IntBuffer) null
        );
        GlStateManager.bindFramebuffer(FramebufferInfo.FRAME_BUFFER, this.fbo);
        GlStateManager.framebufferTexture2D(
            FramebufferInfo.FRAME_BUFFER,
            FramebufferInfo.COLOR_ATTACHMENT,
            GL11.GL_TEXTURE_2D,
            this.colorAttachment,
            0
        );
        if (this.useDepthAttachment) {
            if (isStencilBufferEnabled) {
                GlStateManager.framebufferTexture2D(
                    FramebufferInfo.FRAME_BUFFER,
                    GL30.GL_DEPTH_STENCIL_ATTACHMENT,
                    GL11.GL_TEXTURE_2D,
                    this.depthAttachment,
                    0
                );
            }
            else {
                GlStateManager.framebufferTexture2D(
                    FramebufferInfo.FRAME_BUFFER,
                    FramebufferInfo.DEPTH_ATTACHMENT,
                    GL11.GL_TEXTURE_2D,
                    this.depthAttachment,
                    0
                );
            }
        }
        
        this.checkFramebufferStatus();
        this.clear(getError);
        this.endRead();
    }

//    @Inject(
//        method = "initFbo",
//        at = @At(
//            value = "INVOKE",
//            target = "Lcom/mojang/blaze3d/platform/GlStateManager;bindRenderbuffer(II)V"
//        ),
//        cancellable = true
//    )
//    private void onInitFrameBuffer(int int_1, int int_2, boolean isMac, CallbackInfo ci) {
//        if (isStencilBufferEnabled) {
//            Framebuffer this_ = (Framebuffer) (Object) this;
//
//            GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, this_.depthAttachment);
//            GL30.glRenderbufferStorage(
//                GL30.GL_RENDERBUFFER,
//                org.lwjgl.opengl.EXTPackedDepthStencil.GL_DEPTH24_STENCIL8_EXT,
//                this_.textureWidth,
//                this_.textureHeight
//            );
//            GL30.glFramebufferRenderbuffer(
//                GL30.GL_FRAMEBUFFER,
//                org.lwjgl.opengl.EXTFramebufferObject.GL_DEPTH_ATTACHMENT_EXT,
//                GL30.GL_RENDERBUFFER,
//                this_.depthAttachment
//            );
//            GL30.glFramebufferRenderbuffer(
//                GL30.GL_FRAMEBUFFER,
//                org.lwjgl.opengl.EXTFramebufferObject.GL_STENCIL_ATTACHMENT_EXT,
//                GL30.GL_RENDERBUFFER,
//                this_.depthAttachment
//            );
//
//            this_.checkFramebufferStatus();
//            this_.clear(isMac);
//            this_.endRead();
//
//            CHelper.checkGlError();
//
//            Helper.log("Frame Buffer Reloaded with Stencil Buffer");
//
//            ci.cancel();
//        }
//    }
    
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
