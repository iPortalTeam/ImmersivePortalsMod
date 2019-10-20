package com.qouteall.immersive_portals.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.exposer.IEMinecraftClient;
import com.qouteall.immersive_portals.portal.Portal;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GlFramebuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

public class RendererUsingFrameBuffer extends PortalRenderer {
    SecondaryFrameBuffer secondaryFrameBuffer = new SecondaryFrameBuffer();
    
    @Override
    public void onBeforeTranslucentRendering() {
        renderPortals();
    }
    
    @Override
    public void onAfterTranslucentRendering() {
    
    }
    
    @Override
    public void onRenderCenterEnded() {
    
    }
    
    @Override
    public void finishRendering() {
    
    }
    
    @Override
    public void prepareRendering() {
        secondaryFrameBuffer.prepare();
    
        GlStateManager.enableDepthTest();
    
        GL11.glDisable(GL11.GL_STENCIL_TEST);
    
        if (CGlobal.shaderManager == null) {
            CGlobal.shaderManager = new ShaderManager();
        }
    }
    
    @Override
    protected void doRenderPortal(Portal portal) {
        if (isRendering()) {
            //only support one-layer portal
            return;
        }
        
        if (!testShouldRenderPortal(portal)) {
            return;
        }
        
        portalLayers.push(portal);
        
        GlFramebuffer oldFrameBuffer = mc.getFramebuffer();
    
        ((IEMinecraftClient) mc).setFrameBuffer(secondaryFrameBuffer.fb);
        secondaryFrameBuffer.fb.beginWrite(true);
        
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
        return QueryManager.renderAndGetDoesAnySamplePassed(() -> {
            GlStateManager.enableDepthTest();
            GlStateManager.depthMask(false);
            RenderHelper.setupCameraTransformation();
            GL20.glUseProgram(0);
            ViewAreaRenderer.drawPortalViewTriangle(portal);
            GlStateManager.depthMask(true);
        });
    }
    
    private void renderSecondBufferIntoMainBuffer(Portal portal) {
        RenderHelper.drawFrameBufferUp(portal, secondaryFrameBuffer.fb, CGlobal.shaderManager);
    }
    
}
