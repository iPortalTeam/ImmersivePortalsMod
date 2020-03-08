package com.qouteall.immersive_portals.optifine_compatibility;

import com.mojang.blaze3d.platform.GlStateManager;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.OFInterface;
import com.qouteall.immersive_portals.ducks.IEFrameBuffer;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.render.MyRenderHelper;
import com.qouteall.immersive_portals.render.PortalRenderer;
import com.qouteall.immersive_portals.render.QueryManager;
import com.qouteall.immersive_portals.render.SecondaryFrameBuffer;
import com.qouteall.immersive_portals.render.ShaderManager;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import net.optifine.shaders.Shaders;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_EQUAL;
import static org.lwjgl.opengl.GL11.GL_INCR;
import static org.lwjgl.opengl.GL11.GL_KEEP;
import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL11.GL_STENCIL_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_STENCIL_TEST;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glStencilFunc;
import static org.lwjgl.opengl.GL11.glStencilOp;

public class RendererMixed extends PortalRenderer {
    private SecondaryFrameBuffer[] deferredFbs;
    
    @Override
    public boolean shouldSkipClearing() {
        return false;
    }
    
    @Override
    public void onRenderCenterEnded(MatrixStack matrixStack) {
        int portalLayer = getPortalLayer();
        
        initStencilForLayer(portalLayer);
        
        deferredFbs[portalLayer].fb.beginWrite(true);
        
        glEnable(GL_STENCIL_TEST);
        glStencilFunc(GL_EQUAL, portalLayer, 0xFF);
        glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP);
        
        Framebuffer mcFrameBuffer = mc.getFramebuffer();
        MyRenderHelper.myDrawFrameBuffer(mcFrameBuffer, false, true);
        
        glDisable(GL_STENCIL_TEST);
        
        renderPortals(matrixStack);
    }
    
    private void initStencilForLayer(int portalLayer) {
        if (portalLayer == 0) {
            deferredFbs[portalLayer].fb.beginWrite(true);
            GlStateManager.clearStencil(0);
            GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
        }
        else {
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, deferredFbs[portalLayer - 1].fb.fbo);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, deferredFbs[portalLayer].fb.fbo);
            
            GL30.glBlitFramebuffer(
                0, 0, deferredFbs[0].fb.viewportWidth, deferredFbs[0].fb.viewportHeight,
                0, 0, deferredFbs[0].fb.viewportWidth, deferredFbs[0].fb.viewportHeight,
                GL_STENCIL_BUFFER_BIT, GL_NEAREST
            );
        }
    }
    
    @Override
    public void onBeforeTranslucentRendering(MatrixStack matrixStack) {
    
    }
    
    @Override
    public void onAfterTranslucentRendering(MatrixStack matrixStack) {
        OFHelper.copyFromShaderFbTo(
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
            ((IEFrameBuffer) deferredFb.fb).setIsStencilBufferEnabledAndReload(true);
            
            deferredFb.fb.beginWrite(true);
            GlStateManager.clearColor(1, 0, 1, 0);
            GlStateManager.clearDepth(1);
            GlStateManager.clearStencil(0);
            GL11.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);
            
        }
        
        OFInterface.bindToShaderFrameBuffer.run();
    }
    
    @Override
    public void finishRendering() {
        GlStateManager.colorMask(true, true, true, true);
        Shaders.useProgram(Shaders.ProgramNone);
        //GuiLighting.disable();
        
        if (MyRenderHelper.getRenderedPortalNum() == 0) {
            return;
        }
        
        Framebuffer mainFrameBuffer = mc.getFramebuffer();
        mainFrameBuffer.beginWrite(true);
        
        deferredFbs[0].fb.draw(mainFrameBuffer.viewportWidth, mainFrameBuffer.viewportHeight);
    }
    
    @Override
    protected void doRenderPortal(Portal portal, MatrixStack matrixStack) {
        
        //write to deferred buffer
        if (!tryRenderViewAreaInDeferredBufferAndIncreaseStencil(portal)) {
            return;
        }
        
        portalLayers.push(portal);
        
        OFInterface.bindToShaderFrameBuffer.run();
        manageCameraAndRenderPortalContent(portal);
        
        int innerLayer = getPortalLayer();
        
        portalLayers.pop();
        
        int outerLayer = getPortalLayer();
        
        if (innerLayer > maxPortalLayer.get()) {
            return;
        }
        
        deferredFbs[outerLayer].fb.beginWrite(true);
        
        GlStateManager.enableAlphaTest();
        MyRenderHelper.myDrawFrameBuffer(
            deferredFbs[innerLayer].fb,
            true,
            true
        );
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
            
            OFInterface.bindToShaderFrameBuffer.run();
        });
    }
    
    //maybe it's similar to rendererUsingStencil's ?
    private void myDrawPortalViewArea(Portal portal) {
        assert false;
        
        GlStateManager.enableDepthTest();
        GlStateManager.disableTexture();
        GlStateManager.colorMask(false, false, false, false);
        
        //MyRenderHelper.setupCameraTransformation();
        GL20.glUseProgram(0);
        
        //ViewAreaRenderer.drawPortalViewTriangle(portal);
        
        GlStateManager.enableTexture();
        GlStateManager.colorMask(true, true, true, true);
    }
    
    @Override
    protected void renderPortalContentWithContextSwitched(
        Portal portal, Vec3d oldCameraPos, ClientWorld oldWorld
    ) {
        OFGlobal.shaderContextManager.switchContextAndRun(
            () -> {
                OFInterface.bindToShaderFrameBuffer.run();
                super.renderPortalContentWithContextSwitched(portal, oldCameraPos, oldWorld);
            }
        );
    }
    
    @Override
    public void renderPortalInEntityRenderer(Portal portal) {
        assert false;
//        if (Shaders.isShadowPass) {
//            ViewAreaRenderer.drawPortalViewTriangle(portal);
//        }
    }
}
