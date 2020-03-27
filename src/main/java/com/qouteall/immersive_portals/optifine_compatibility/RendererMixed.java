package com.qouteall.immersive_portals.optifine_compatibility;

import com.mojang.blaze3d.platform.GlStateManager;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.CHelper;
import com.qouteall.immersive_portals.ducks.IEFrameBuffer;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.render.MyRenderHelper;
import com.qouteall.immersive_portals.render.PortalRenderer;
import com.qouteall.immersive_portals.render.QueryManager;
import com.qouteall.immersive_portals.render.SecondaryFrameBuffer;
import com.qouteall.immersive_portals.render.ShaderManager;
import com.qouteall.immersive_portals.render.ViewAreaRenderer;
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
    private SecondaryFrameBuffer[] deferredFbs = new SecondaryFrameBuffer[0];
    
    //OptiFine messes up with transformations so store it
    private MatrixStack modelView = new MatrixStack();
    
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
        
        Framebuffer mcFrameBuffer = client.getFramebuffer();
        MyRenderHelper.myDrawFrameBuffer(mcFrameBuffer, false, true);
        
        glDisable(GL_STENCIL_TEST);
        
        MatrixStack effectiveTransformation = this.modelView;
        modelView = new MatrixStack();
        
        renderPortals(effectiveTransformation);
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
        
        modelView.push();
        modelView.peek().getModel().multiply(matrixStack.peek().getModel());
        modelView.peek().getNormal().multiply(matrixStack.peek().getNormal());
    }
    
    @Override
    public void prepareRendering() {
        if (CGlobal.shaderManager == null) {
            CGlobal.shaderManager = new ShaderManager();
        }
        
        if (deferredFbs.length != maxPortalLayer.get() + 1) {
            for (SecondaryFrameBuffer fb : deferredFbs) {
                fb.fb.delete();
            }
            
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
        
        OFGlobal.bindToShaderFrameBuffer.run();
    }
    
    @Override
    public void finishRendering() {
        GlStateManager.colorMask(true, true, true, true);
        Shaders.useProgram(Shaders.ProgramNone);
        //GuiLighting.disable();
        
        if (MyRenderHelper.getRenderedPortalNum() == 0) {
            return;
        }
        
        Framebuffer mainFrameBuffer = client.getFramebuffer();
        mainFrameBuffer.beginWrite(true);
        
        deferredFbs[0].fb.draw(mainFrameBuffer.viewportWidth, mainFrameBuffer.viewportHeight);
        
        CHelper.checkGlError();
    }
    
    @Override
    protected void doRenderPortal(Portal portal, MatrixStack matrixStack) {
        //reset projection matrix
        client.gameRenderer.method_22709(MyRenderHelper.projectionMatrix);
        
        //write to deferred buffer
        if (!tryRenderViewAreaInDeferredBufferAndIncreaseStencil(portal, matrixStack)) {
            return;
        }
        
        portalLayers.push(portal);
        
        OFGlobal.bindToShaderFrameBuffer.run();
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
    
    private boolean tryRenderViewAreaInDeferredBufferAndIncreaseStencil(
        Portal portal, MatrixStack matrixStack
    ) {
        int portalLayer = getPortalLayer();
        
        initStencilForLayer(portalLayer);
        
        deferredFbs[portalLayer].fb.beginWrite(true);
        
        GL20.glUseProgram(0);
        
        GL11.glEnable(GL_STENCIL_TEST);
        GL11.glStencilFunc(GL11.GL_EQUAL, portalLayer, 0xFF);
        GL11.glStencilOp(GL_KEEP, GL_KEEP, GL_INCR);
        
        GlStateManager.enableDepthTest();
        
        boolean result = QueryManager.renderAndGetDoesAnySamplePassed(() -> {
            ViewAreaRenderer.drawPortalViewTriangle(
                portal, matrixStack, true, true
            );
        });
        
        GL11.glDisable(GL_STENCIL_TEST);
        
        OFGlobal.bindToShaderFrameBuffer.run();
        
        return result;
    }
    
    @Override
    protected void renderPortalContentWithContextSwitched(
        Portal portal, Vec3d oldCameraPos, ClientWorld oldWorld
    ) {
        OFGlobal.shaderContextManager.switchContextAndRun(
            () -> {
                OFGlobal.bindToShaderFrameBuffer.run();
                super.renderPortalContentWithContextSwitched(portal, oldCameraPos, oldWorld);
            }
        );
    }
    
    @Override
    public void renderPortalInEntityRenderer(Portal portal) {
        //TODO render shadow
//        if (Shaders.isShadowPass) {
//            ViewAreaRenderer.drawPortalViewTriangle(portal);
//        }
    }
}
