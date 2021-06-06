package qouteall.imm_ptl.core.optifine_compatibility;

import com.mojang.blaze3d.platform.GlStateManager;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.ducks.IEFrameBuffer;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalLike;
import qouteall.imm_ptl.core.render.MyGameRenderer;
import qouteall.imm_ptl.core.render.MyRenderHelper;
import qouteall.imm_ptl.core.render.PortalRenderer;
import qouteall.imm_ptl.core.render.SecondaryFrameBuffer;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;
import qouteall.imm_ptl.core.render.context_management.RenderStates;
import qouteall.imm_ptl.core.render.context_management.WorldRenderInfo;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.util.math.MatrixStack;
import net.optifine.shaders.Shaders;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_EQUAL;
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
    
    private boolean portalRenderingNeeded = false;
    private boolean nextFramePortalRenderingNeeded = false;
    
    public RendererMixed() {
        IPGlobal.preGameRenderSignal.connect(() -> {
            updateNeedsPortalRendering();
        });
    }
    
    @Override
    public boolean replaceFrameBufferClearing() {
        return false;
    }
    
    @Override
    public void onRenderCenterEnded(MatrixStack matrixStack) {
        // avoid this thing needs to be invoked when no portal is rendered
        // it may cost performance
        if (portalRenderingNeeded) {
            int portalLayer = PortalRendering.getPortalLayer();
            
            initStencilForLayer(portalLayer);
            
            deferredFbs[portalLayer].fb.beginWrite(true);
            deferredFbs[portalLayer].fb.checkFramebufferStatus();
            
            glEnable(GL_STENCIL_TEST);
            glStencilFunc(GL_EQUAL, portalLayer, 0xFF);
            glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP);
            
            Framebuffer mcFrameBuffer = client.getFramebuffer();
            
            MyRenderHelper.clearAlphaTo1(mcFrameBuffer);
            
            deferredFbs[portalLayer].fb.beginWrite(true);
            deferredFbs[portalLayer].fb.checkFramebufferStatus();
            MyRenderHelper.drawScreenFrameBuffer(mcFrameBuffer, false, true);
            
            glDisable(GL_STENCIL_TEST);
            
            deferredFbs[portalLayer].fb.endWrite();
        }
        
        MatrixStack effectiveTransformation = this.modelView;
        modelView = new MatrixStack();
        
        renderPortals(effectiveTransformation);
    }
    
    private void initStencilForLayer(int portalLayer) {
        if (portalLayer == 0) {
            deferredFbs[portalLayer].fb.beginWrite(true);
            deferredFbs[portalLayer].fb.checkFramebufferStatus();
            GlStateManager._clearStencil(0);
            GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
        }
        else {
            deferredFbs[portalLayer - 1].fb.beginWrite(false);
            deferredFbs[portalLayer - 1].fb.checkFramebufferStatus();
            deferredFbs[portalLayer].fb.beginWrite(false);
            deferredFbs[portalLayer].fb.checkFramebufferStatus();
            
            
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
        if (portalRenderingNeeded) {
            deferredFbs[PortalRendering.getPortalLayer()].fb.beginWrite(false);
            deferredFbs[PortalRendering.getPortalLayer()].fb.checkFramebufferStatus();
            
            OFHelper.copyFromShaderFbTo(
                deferredFbs[PortalRendering.getPortalLayer()].fb,
                GL_DEPTH_BUFFER_BIT
            );
        }
        
        modelView.push();
        modelView.peek().getModel().multiply(matrixStack.peek().getModel());
        modelView.peek().getNormal().multiply(matrixStack.peek().getNormal());
    }
    
    @Override
    public void prepareRendering() {
        if (deferredFbs.length != PortalRendering.getMaxPortalLayer() + 1) {
            for (SecondaryFrameBuffer fb : deferredFbs) {
                fb.fb.delete();
            }
            
            deferredFbs = new SecondaryFrameBuffer[PortalRendering.getMaxPortalLayer() + 1];
            for (int i = 0; i < deferredFbs.length; i++) {
                deferredFbs[i] = new SecondaryFrameBuffer();
            }
        }
        
        CHelper.checkGlError();
        
        for (SecondaryFrameBuffer deferredFb : deferredFbs) {
            deferredFb.prepare();
            ((IEFrameBuffer) deferredFb.fb).setIsStencilBufferEnabledAndReload(true);
            
            deferredFb.fb.beginWrite(true);
            GlStateManager._clearColor(1, 0, 1, 0);
            GlStateManager._clearDepth(1);
            GlStateManager._clearStencil(0);
            GL11.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);
            
            CHelper.checkGlError();
            
            deferredFb.fb.endWrite();
        }
    }
    
    private void updateNeedsPortalRendering() {
        portalRenderingNeeded = nextFramePortalRenderingNeeded;
        nextFramePortalRenderingNeeded = false;
    }
    
    @Override
    public void finishRendering() {
        GlStateManager._colorMask(true, true, true, true);
        Shaders.useProgram(Shaders.ProgramNone);
        
        if (RenderStates.getRenderedPortalNum() == 0) {
            return;
        }
        
        if (!portalRenderingNeeded) {
            return;
        }
        
        Framebuffer mainFrameBuffer = client.getFramebuffer();
        mainFrameBuffer.beginWrite(true);
        mainFrameBuffer.checkFramebufferStatus();
        
        deferredFbs[0].fb.draw(mainFrameBuffer.viewportWidth, mainFrameBuffer.viewportHeight);
        
        CHelper.checkGlError();
    }
    
    @Override
    protected void doRenderPortal(PortalLike portal, MatrixStack matrixStack) {
        nextFramePortalRenderingNeeded = true;
        
        if (!portalRenderingNeeded) {
            return;
        }
        
        //reset projection matrix
        client.gameRenderer.loadProjectionMatrix(RenderStates.projectionMatrix);
        
        //write to deferred buffer
        if (!tryRenderViewAreaInDeferredBufferAndIncreaseStencil(portal, matrixStack)) {
            return;
        }
        
        PortalRendering.pushPortalLayer(portal);
        
//        OFGlobal.bindToShaderFrameBuffer.run();
        renderPortalContent(portal);
        
        int innerLayer = PortalRendering.getPortalLayer();
        
        PortalRendering.popPortalLayer();
        
        int outerLayer = PortalRendering.getPortalLayer();
        
        if (innerLayer > PortalRendering.getMaxPortalLayer()) {
            return;
        }
        
        deferredFbs[outerLayer].fb.beginWrite(true);
        deferredFbs[outerLayer].fb.checkFramebufferStatus();
        
        MyRenderHelper.drawScreenFrameBuffer(
            deferredFbs[innerLayer].fb,
            true,
            true
        );
    }
    
    private boolean tryRenderViewAreaInDeferredBufferAndIncreaseStencil(
        PortalLike portal, MatrixStack matrixStack
    ) {
        throw new RuntimeException();
//        int portalLayer = PortalRendering.getPortalLayer();
//
//        initStencilForLayer(portalLayer);
//
//        deferredFbs[portalLayer].fb.beginWrite(true);
//        deferredFbs[portalLayer].fb.checkFramebufferStatus();
//
//        GL20.glUseProgram(0);
//
//        GL11.glEnable(GL_STENCIL_TEST);
//        GL11.glStencilFunc(GL11.GL_EQUAL, portalLayer, 0xFF);
//        GL11.glStencilOp(GL_KEEP, GL_KEEP, GL_INCR);
//
//        GlStateManager._enableDepthTest();
//
//        boolean result = QueryManager.renderAndGetDoesAnySamplePass(() -> {
//            ViewAreaRenderer.drawPortalViewTriangle(
//                portal, matrixStack, true, true
//            );
//        });
//
//        GL11.glDisable(GL_STENCIL_TEST);
//
//        OFGlobal.bindToShaderFrameBuffer.run();
//
//        return result;
    }
    
    @Override
    public void invokeWorldRendering(
        WorldRenderInfo worldRenderInfo
    ) {
        MyGameRenderer.renderWorldNew(
            worldRenderInfo,
            runnable -> {
                OFGlobal.shaderContextManager.switchContextAndRun(() -> {
//                    OFGlobal.bindToShaderFrameBuffer.run();
                    runnable.run();
                });
            }
        );
    }
    
    @Override
    public void renderPortalInEntityRenderer(Portal portal) {
//        if (Shaders.isShadowPass) {
//            ViewAreaRenderer.drawPortalViewTriangle(portal);
//        }
    }
}