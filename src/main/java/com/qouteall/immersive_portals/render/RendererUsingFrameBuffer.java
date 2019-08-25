package com.qouteall.immersive_portals.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.qouteall.immersive_portals.exposer.IEMinecraftClient;
import com.qouteall.immersive_portals.my_util.Helper;
import com.qouteall.immersive_portals.portal.Portal;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GlFramebuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

public class RendererUsingFrameBuffer extends PortalRenderer {
    private GlFramebuffer secondFrameBuffer;
    private int width;
    private int height;
    private ShaderManager shaderManager;
    
    @Override
    protected void initIfNeeded() {
        super.initIfNeeded();
        if (secondFrameBuffer == null) {
            width = mc.window.getFramebufferWidth();
            height = mc.window.getFramebufferHeight();
            secondFrameBuffer = new GlFramebuffer(
                width,
                height,
                true,
                MinecraftClient.IS_SYSTEM_MAC
            );
        }
        if (shaderManager == null) {
            shaderManager = new ShaderManager();
        }
    }
    
    @Override
    protected void prepareStates() {
        if (mc.window.getFramebufferWidth() != width || mc.window.getFramebufferHeight() != height) {
            Helper.log("Second Frame Buffer Resized");
            secondFrameBuffer.resize(
                mc.window.getFramebufferWidth(),
                mc.window.getFramebufferHeight(),
                MinecraftClient.IS_SYSTEM_MAC
            );
            width = mc.window.getFramebufferWidth();
            height = mc.window.getFramebufferHeight();
        }
        GlStateManager.enableDepthTest();
        
        GL11.glDisable(GL11.GL_STENCIL_TEST);
    }
    
    @Override
    protected void doRenderPortal(Portal portal) {
        if (isRendering()) {
            //currently only support one-layer portal
            return;
        }
        
        if (!testShouldRenderPortal(portal)) {
            return;
        }
        
        portalLayers.push(portal);
        
        GlFramebuffer oldFrameBuffer = mc.getFramebuffer();
        
        ((IEMinecraftClient) mc).setFrameBuffer(secondFrameBuffer);
        secondFrameBuffer.beginWrite(true);
        
        GlStateManager.clearColor(1, 0, 1, 1);
        GlStateManager.clearDepth(1);
        GlStateManager.clear(
            GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT,
            MinecraftClient.IS_SYSTEM_MAC
        );
        GL11.glDisable(GL11.GL_STENCIL_TEST);
        
        manageCameraAndRenderPortalContent(portal);
        
        ((IEMinecraftClient) mc).setFrameBuffer(oldFrameBuffer);
        oldFrameBuffer.beginWrite(true);
        
        portalLayers.pop();
        
        renderSecondBufferIntoMainBuffer(portal);
    }
    
    @Override
    public void renderPortalInEntityRenderer(Portal portal) {
        //nothing
    }
    
    @Override
    public boolean shouldSkipClearing() {
        return false;
    }
    
    private boolean testShouldRenderPortal(Portal portal) {
        return renderAndGetDoesAnySamplePassed(() -> {
            GlStateManager.enableDepthTest();
            GlStateManager.depthMask(false);
            setupCameraTransformation();
            GL20.glUseProgram(0);
            drawPortalViewTriangle(portal);
            GlStateManager.depthMask(true);
        });
    }
    
    private void renderSecondBufferIntoMainBuffer(Portal portal) {
        GlFramebuffer textureProvider = this.secondFrameBuffer;
    
        drawFrameBufferUp(portal, textureProvider, shaderManager);
    }
    
}
