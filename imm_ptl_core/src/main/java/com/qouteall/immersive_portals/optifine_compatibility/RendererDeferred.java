package com.qouteall.immersive_portals.optifine_compatibility;

import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.CHelper;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.portal.PortalLike;
import com.qouteall.immersive_portals.render.MyGameRenderer;
import com.qouteall.immersive_portals.render.MyRenderHelper;
import com.qouteall.immersive_portals.render.PortalRenderer;
import com.qouteall.immersive_portals.render.SecondaryFrameBuffer;
import com.qouteall.immersive_portals.render.context_management.PortalRendering;
import com.qouteall.immersive_portals.render.context_management.WorldRenderInfo;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import static org.lwjgl.opengl.GL11.GL_NEAREST;

public class RendererDeferred extends PortalRenderer {
    private SecondaryFrameBuffer deferredBuffer = new SecondaryFrameBuffer();
    private MatrixStack modelView = new MatrixStack();
    
    @Override
    public boolean replaceFrameBufferClearing() {
        return false;
    }
    
    @Override
    public void onBeforeTranslucentRendering(MatrixStack matrixStack) {
        if (PortalRendering.isRendering()) {
            return;
        }
        modelView.push();
        modelView.peek().getModel().multiply(matrixStack.peek().getModel());
        modelView.peek().getNormal().multiply(matrixStack.peek().getNormal());
    }
    
    @Override
    public void onAfterTranslucentRendering(MatrixStack matrixStack) {
    
    }
    
    @Override
    public void finishRendering() {
    
    }
    
    @Override
    public void prepareRendering() {
        deferredBuffer.prepare();
        
        deferredBuffer.fb.setClearColor(1, 0, 0, 0);
        deferredBuffer.fb.clear(MinecraftClient.IS_SYSTEM_MAC);
        
        OFGlobal.bindToShaderFrameBuffer.run();
        
    }
    
    @Override
    protected void doRenderPortal(PortalLike portal, MatrixStack matrixStack) {
        throw new RuntimeException();
        
//        if (PortalRendering.isRendering()) {
//            //currently only support one-layer portal
//            return;
//        }
//
//
//        if (!testShouldRenderPortal(portal, matrixStack)) {
//            return;
//        }
//
//        OFGlobal.bindToShaderFrameBuffer.run();
//
//        PortalRendering.pushPortalLayer(portal);
//
//        renderPortalContent(portal);
//
//        PortalRendering.popPortalLayer();
//
//        RenderSystem.enableDepthTest();
//
//        GlStateManager._activeTexture(GL13.GL_TEXTURE0);
//
//        client.gameRenderer.loadProjectionMatrix(RenderStates.projectionMatrix);
//
//        OFInterface.resetViewport.run();
//
//        deferredBuffer.fb.beginWrite(true);
//        MyRenderHelper.drawFrameBufferUp(
//            portal,
//            client.getFramebuffer(),
//            matrixStack
//        );
//
//        RenderSystem.colorMask(true, true, true, true);
//
//        OFGlobal.bindToShaderFrameBuffer.run();
    }
    
    @Override
    public void invokeWorldRendering(
        WorldRenderInfo worldRenderInfo
    ) {
        MyGameRenderer.renderWorldNew(
            worldRenderInfo,
            runnable -> {
                OFGlobal.shaderContextManager.switchContextAndRun(() -> {
                    OFGlobal.bindToShaderFrameBuffer.run();
                    runnable.run();
                });
            }
        );
    }
    
    @Override
    public void renderPortalInEntityRenderer(Portal portal) {
    
    }
    
    private boolean testShouldRenderPortal(PortalLike portal, MatrixStack matrixStack) {
        throw new RuntimeException();
//        //reset projection matrix
//        client.gameRenderer.loadProjectionMatrix(RenderStates.projectionMatrix);
//
//        deferredBuffer.fb.beginWrite(true);
//        return QueryManager.renderAndGetDoesAnySamplePass(() -> {
//            GlStateManager._enableDepthTest();
//
//            GlStateManager._disableTexture();
//            GlStateManager._colorMask(false, false, false, false);
//
//            GlStateManager._depthMask(false);
//            GL20.glUseProgram(0);
//
//            GlStateManager._disableTexture();
//
//            ViewAreaRenderer.drawPortalViewTriangle(
//                portal, matrixStack, true, true
//            );
//
//            GlStateManager._enableTexture();
//            GlStateManager._colorMask(true, true, true, true);
//            GlStateManager._depthMask(true);
//        });
    }
    
    @Override
    public void onRenderCenterEnded(MatrixStack matrixStack) {
        if (PortalRendering.isRendering()) {
            return;
        }
        
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, client.getFramebuffer().fbo);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, deferredBuffer.fb.fbo);
        GL30.glBlitFramebuffer(
            0, 0, deferredBuffer.fb.textureWidth, deferredBuffer.fb.textureHeight,
            0, 0, deferredBuffer.fb.textureWidth, deferredBuffer.fb.textureHeight,
            GL11.GL_COLOR_BUFFER_BIT, GL_NEAREST
        );
        
        CHelper.checkGlError();
        
        OFHelper.copyFromShaderFbTo(deferredBuffer.fb, GL11.GL_DEPTH_BUFFER_BIT);
        
        renderPortals(modelView);
        modelView.pop();
        
        Framebuffer mainFrameBuffer = client.getFramebuffer();
        mainFrameBuffer.beginWrite(true);

        MyRenderHelper.drawScreenFrameBuffer(
            deferredBuffer.fb,
            false,
            false
        );
    }
}
