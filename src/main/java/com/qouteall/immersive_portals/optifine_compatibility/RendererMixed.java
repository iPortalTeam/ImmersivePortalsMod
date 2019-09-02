package com.qouteall.immersive_portals.optifine_compatibility;

import com.mojang.blaze3d.platform.GlStateManager;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.exposer.IEGlFrameBuffer;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.render.*;
import net.minecraft.client.gl.GlFramebuffer;
import net.minecraft.util.math.Vec3d;
import net.optifine.shaders.Shaders;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import static org.lwjgl.opengl.GL11.*;

public class RendererMixed extends PortalRenderer {
    private SecondaryFrameBuffer deferredBuffer = new SecondaryFrameBuffer();
    public static final int maxPortalLayer = 3;
    
    private SecondaryFrameBuffer[] secondaryFbs = new SecondaryFrameBuffer[maxPortalLayer];
    
    @Override
    public boolean shouldSkipClearing() {
        return false;
    }
    
    @Override
    public void onRenderCenterEnded() {
        if (isRendering()) {
            renderPortals();
        }
    }
    
    @Override
    public void onBeforeTranslucentRendering() {
    
    }
    
    @Override
    public void onAfterTranslucentRendering() {
        copyDepthFromShaderToDeferred();
    
        if (!isRendering()) {
            renderPortals();
        }
    }
    
    @Override
    public void prepareRendering() {
        if (CGlobal.shaderManager == null) {
            CGlobal.shaderManager = new ShaderManager();
        }
        
        deferredBuffer.prepare();
        ((IEGlFrameBuffer) deferredBuffer.fb).setIsStencilBufferEnabledAndReload(true);
    
        deferredBuffer.fb.beginWrite(true);
        GlStateManager.clearColor(1, 0, 0, 0);
        GlStateManager.clearDepth(1);
        GlStateManager.clearStencil(0);
        GlStateManager.clear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);
        
        OFHelper.bindToShaderFrameBuffer();
        
        GlStateManager.viewport(0, 0, Shaders.renderWidth, Shaders.renderHeight);
    }
    
    @Override
    public void finishRendering() {
        if (RenderHelper.renderedPortalNum == 0) {
            return;
        }
        
        GlStateManager.enableAlphaTest();
        GlFramebuffer mainFrameBuffer = mc.getFramebuffer();
        mainFrameBuffer.beginWrite(true);
        
        CGlobal.doDisableAlphaTestWhenRenderingFrameBuffer = false;
        deferredBuffer.fb.draw(mainFrameBuffer.viewWidth, mainFrameBuffer.viewHeight);
        CGlobal.doDisableAlphaTestWhenRenderingFrameBuffer = true;
    }
    
    //this involves 3 frame buffers:
    //mc framebuffer, shader framebuffer(in current dimension), my deferred buffer
    @Override
    protected void doRenderPortal(Portal portal) {
        
        //write to deferred buffer
        if (!tryRenderViewAreaInDeferredBufferAndIncreaseStencil(portal)) {
            return;
        }
        
        portalLayers.push(portal);
        
        //write to shader fb in dest dimension
        //then write to mc fb
        manageCameraAndRenderPortalContent(portal);
        
        int innerStencilValue = getPortalLayer();
        
        portalLayers.pop();
        
        deferredBuffer.fb.beginWrite(true);
        
        writeFromMcFbToDeferredFb(portal, innerStencilValue);

//        //make depth correct in shader fb
//        OFHelper.bindToShaderFrameBuffer();
//        myDrawPortalViewArea(portal);
    
        if (getPortalLayer() < 3) {
            renderPortals();
        }
    
        int outerStencilValue = getPortalLayer();
        GL11.glEnable(GL_STENCIL_TEST);
        RendererUsingStencil.clampStencilValue(outerStencilValue);
        GL11.glDisable(GL_STENCIL_TEST);
    }
    
    //drawing will be limited by stencil and will increase stencil
    private void writeFromMcFbToDeferredFb(Portal portal, int innerStencilValue) {
        GL11.glEnable(GL_STENCIL_TEST);
        GL11.glStencilFunc(GL11.GL_EQUAL, innerStencilValue, 0xFF);
        GL11.glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP);
    
        GlStateManager.disableDepthTest();
        GlStateManager.depthMask(false);
        
        RenderHelper.drawFrameBufferUp(portal, mc.getFramebuffer(), CGlobal.shaderManager);
    
        GlStateManager.enableDepthTest();
        GlStateManager.depthMask(true);
        
        GL11.glDisable(GL_STENCIL_TEST);
    }
    
    //TODO reduce repeat
    private void copyDepthFromShaderToDeferred() {
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, OFGlobal.getDfb.get());
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, deferredBuffer.fb.fbo);
        
        GL30.glBlitFramebuffer(
            0, 0, Shaders.renderWidth, Shaders.renderHeight,
            0, 0, deferredBuffer.fb.viewWidth, deferredBuffer.fb.viewHeight,
            GL11.GL_DEPTH_BUFFER_BIT, GL11.GL_NEAREST
        );
        
        OFHelper.bindToShaderFrameBuffer();
    }
    
    //NOTE it will write to shader depth buffer
    //it's drawing into shader fb
    private boolean tryRenderViewAreaInDeferredBufferAndIncreaseStencil(Portal portal) {
        return QueryManager.renderAndGetDoesAnySamplePassed(() -> {
            deferredBuffer.fb.beginWrite(true);
    
            int outerStencilValue = getPortalLayer();
            GL11.glEnable(GL_STENCIL_TEST);
            GL11.glStencilFunc(GL11.GL_EQUAL, outerStencilValue, 0xFF);
            GL11.glStencilOp(GL_KEEP, GL_KEEP, GL_INCR);
            
            myDrawPortalViewArea(portal);
    
            GL11.glDisable(GL_STENCIL_TEST);
            
            OFHelper.bindToShaderFrameBuffer();
        });
    }
    
    //maybe it's similar to rendererUsingStencil's ?
    private void myDrawPortalViewArea(Portal portal) {
        GlStateManager.enableDepthTest();
        GlStateManager.disableTexture();
        GlStateManager.colorMask(false, false, false, false);
        
        RenderHelper.setupCameraTransformation();
        GL20.glUseProgram(0);
        
        RenderHelper.drawPortalViewTriangle(portal);
        
        GlStateManager.enableTexture();
        GlStateManager.colorMask(true, true, true, true);
    }
    
    @Override
    protected void renderPortalContentWithContextSwitched(
        Portal portal, Vec3d oldCameraPos
    ) {
        OFGlobal.shaderContextManager.switchContextAndRun(
            () -> {
                OFHelper.bindToShaderFrameBuffer();
                super.renderPortalContentWithContextSwitched(portal, oldCameraPos);
            }
        );
    }
    
    @Override
    public void renderPortalInEntityRenderer(Portal portal) {
    
    }
}
