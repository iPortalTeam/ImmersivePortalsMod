package com.qouteall.immersive_portals.optifine_compatibility;

import com.mojang.blaze3d.platform.GlStateManager;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.exposer.IEGlFrameBuffer;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.render.*;
import net.minecraft.client.gl.GlFramebuffer;
import net.minecraft.client.render.GuiLighting;
import net.minecraft.util.math.Vec3d;
import net.optifine.shaders.Shaders;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import static org.lwjgl.opengl.GL11.*;

public class RendererMixed extends PortalRenderer {
    private SecondaryFrameBuffer[] deferredFbs;
    
    @Override
    public boolean shouldSkipClearing() {
        return false;
    }
    
    @Override
    public void onRenderCenterEnded() {
        int portalLayer = getPortalLayer();
    
        initStencilForLayer(portalLayer);
    
        deferredFbs[portalLayer].fb.beginWrite(true);
    
        glEnable(GL_STENCIL_TEST);
        glStencilFunc(GL_EQUAL, portalLayer, 0xFF);
        glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP);
    
        GlFramebuffer mcFrameBuffer = mc.getFramebuffer();
        RenderHelper.myDrawFrameBuffer(mcFrameBuffer, false, true);
        
        glDisable(GL_STENCIL_TEST);
    
        renderPortals();
    }
    
    private void initStencilForLayer(int portalLayer) {
        if (portalLayer == 0) {
            deferredFbs[portalLayer].fb.beginWrite(true);
            GlStateManager.clearStencil(0);
            GlStateManager.clear(GL_STENCIL_BUFFER_BIT);
        }
        else {
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, deferredFbs[portalLayer - 1].fb.fbo);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, deferredFbs[portalLayer].fb.fbo);
            
            GL30.glBlitFramebuffer(
                0, 0, deferredFbs[0].fb.viewWidth, deferredFbs[0].fb.viewHeight,
                0, 0, deferredFbs[0].fb.viewWidth, deferredFbs[0].fb.viewHeight,
                GL_STENCIL_BUFFER_BIT, GL_NEAREST
            );
        }
    }
    
    @Override
    public void onBeforeTranslucentRendering() {
    
    }
    
    @Override
    public void onAfterTranslucentRendering() {
        RenderHelper.copyFromShaderFbTo(
            deferredFbs[getPortalLayer()].fb,
            GL_DEPTH_BUFFER_BIT
        );
    }
    
    @Override
    public void prepareRendering() {
        if (CGlobal.shaderManager == null) {
            CGlobal.shaderManager = new ShaderManager();
        }
    
        if (deferredFbs == null) {
            deferredFbs = new SecondaryFrameBuffer[maxPortalLayer.get() + 1];
            for (int i = 0; i < deferredFbs.length; i++) {
                deferredFbs[i] = new SecondaryFrameBuffer();
            }
        }
    
        for (SecondaryFrameBuffer deferredFb : deferredFbs) {
            deferredFb.prepare();
            ((IEGlFrameBuffer) deferredFb.fb).setIsStencilBufferEnabledAndReload(true);
        
            deferredFb.fb.beginWrite(true);
            GlStateManager.clearColor(1, 0, 1, 0);
            GlStateManager.clearDepth(1);
            GlStateManager.clearStencil(0);
            GlStateManager.clear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);
        
        }
        
        OFHelper.bindToShaderFrameBuffer();
    }
    
    @Override
    public void finishRendering() {
        GlStateManager.colorMask(true, true, true, true);
        Shaders.useProgram(Shaders.ProgramNone);
        GuiLighting.disable();
        
        if (RenderHelper.getRenderedPortalNum() == 0) {
            return;
        }

        GlFramebuffer mainFrameBuffer = mc.getFramebuffer();
        mainFrameBuffer.beginWrite(true);
    
        deferredFbs[0].fb.draw(mainFrameBuffer.viewWidth, mainFrameBuffer.viewHeight);
    }
    
    @Override
    protected void doRenderPortal(Portal portal) {
        
        //write to deferred buffer
        if (!tryRenderViewAreaInDeferredBufferAndIncreaseStencil(portal)) {
            return;
        }
        
        portalLayers.push(portal);
    
        OFHelper.bindToShaderFrameBuffer();
        manageCameraAndRenderPortalContent(portal);
    
        int innerLayer = getPortalLayer();
        
        portalLayers.pop();
    
        int outerLayer = getPortalLayer();
    
        deferredFbs[outerLayer].fb.beginWrite(true);
    
        boolean r = QueryManager.renderAndGetDoesAnySamplePassed(() -> {
            GlStateManager.enableAlphaTest();
            RenderHelper.myDrawFrameBuffer(
                deferredFbs[innerLayer].fb,
                true,
                true
            );
        });
    }
    
    //NOTE it will write to shader depth buffer
    //it's drawing into shader fb
    private boolean tryRenderViewAreaInDeferredBufferAndIncreaseStencil(Portal portal) {
        return QueryManager.renderAndGetDoesAnySamplePassed(() -> {
            int portalLayer = getPortalLayer();
    
            initStencilForLayer(portalLayer);
            
            deferredFbs[portalLayer].fb.beginWrite(true);
            
            GL11.glEnable(GL_STENCIL_TEST);
            GL11.glStencilFunc(GL11.GL_EQUAL, portalLayer, 0xFF);
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
        if (Shaders.isShadowPass) {
            RenderHelper.drawPortalViewTriangle(portal);
        }
    }
}
