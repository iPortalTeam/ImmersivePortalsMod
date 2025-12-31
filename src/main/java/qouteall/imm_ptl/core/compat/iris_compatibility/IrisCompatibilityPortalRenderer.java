package qouteall.imm_ptl.core.compat.iris_compatibility;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Util;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.compat.IPPortingLibCompat;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalRenderInfo;
import qouteall.imm_ptl.core.render.MyGameRenderer;
import qouteall.imm_ptl.core.render.MyRenderHelper;
import qouteall.imm_ptl.core.render.SecondaryFrameBuffer;
import qouteall.imm_ptl.core.render.ViewAreaRenderer;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;
import qouteall.imm_ptl.core.render.context_management.WorldRenderInfo;
import qouteall.imm_ptl.core.render.renderer.PortalRenderer;

import java.util.List;

import static org.lwjgl.opengl.GL11.GL_STENCIL_TEST;

public class IrisCompatibilityPortalRenderer extends PortalRenderer {
    
    public static final IrisCompatibilityPortalRenderer instance = new IrisCompatibilityPortalRenderer(false);
    public static final IrisCompatibilityPortalRenderer debugModeInstance =
        new IrisCompatibilityPortalRenderer(true);
    
    private SecondaryFrameBuffer deferredBuffer = new SecondaryFrameBuffer();
    
    // TODO figure out why this field existed in old versions
    private Matrix4f passingModelView = new Matrix4f();
    
    public boolean isDebugMode;
    
    public IrisCompatibilityPortalRenderer(boolean isDebugMode) {
        this.isDebugMode = isDebugMode;
    }
    
    @Override
    public boolean replaceFrameBufferClearing() {
        client.getMainRenderTarget().bindWrite(false);
        
        return false;
    }
    
    @Override
    public void onBeforeTranslucentRendering(Matrix4f modelView) {
        if (PortalRendering.isRendering()) {
            return;
        }
        
        passingModelView = modelView;
        
        GL11.glDisable(GL_STENCIL_TEST);
    }
    
    @Override
    public void onAfterTranslucentRendering(Matrix4f modelView) {
    
    }
    
    @Override
    public void finishRendering() {
        GL11.glDisable(GL_STENCIL_TEST);
    }
    
    @Override
    public void prepareRendering() {
        deferredBuffer.prepare();
        
        deferredBuffer.fb.setClearColor(1, 0, 0, 0);
        deferredBuffer.fb.clear(Util.getPlatform() == Util.OS.OSX);
        
        IPPortingLibCompat.setIsStencilEnabled(
            client.getMainRenderTarget(), false
        );
        
        // Iris now use vanilla framebuffer's depth
        client.getMainRenderTarget().bindWrite(false);
    }
    
    protected void doRenderPortal(Portal portal, Matrix4f modelView) {
        if (PortalRendering.isRendering()) {
            // this renderer only supports one-layer portal
            return;
        }
        
        if (!testShouldRenderPortal(portal, modelView)) {
            return;
        }
        
        client.getMainRenderTarget().bindWrite(true);
        
        PortalRendering.pushPortalLayer(portal);
        
        renderPortalContent(portal);
        
        PortalRendering.popPortalLayer();
        
        CHelper.enableDepthClamp();
        
        if (!isDebugMode) {
            // draw portal content to the deferred buffer
            deferredBuffer.fb.bindWrite(true);
            MyRenderHelper.drawPortalAreaWithFramebuffer(
                portal,
                client.getMainRenderTarget(),
                modelView,
                RenderSystem.getProjectionMatrix()
            );
        }
        else {
            deferredBuffer.fb.bindWrite(true);
            MyRenderHelper.drawScreenFrameBuffer(
                client.getMainRenderTarget(),
                true, true
            );
        }
        
        CHelper.disableDepthClamp();
        
        RenderSystem.colorMask(true, true, true, true);
        
        client.getMainRenderTarget().bindWrite(true);
    }
    
    @Override
    public void invokeWorldRendering(
        WorldRenderInfo worldRenderInfo
    ) {
        MyGameRenderer.renderWorldNew(
            worldRenderInfo,
            Runnable::run
        );
    }
    
    @Override
    public void renderPortalInEntityRenderer(Portal portal) {
    
    }
    
    private boolean testShouldRenderPortal(Portal portal, Matrix4f modelView) {
        
        //reset projection matrix
//        client.gameRenderer.loadProjectionMatrix(RenderStates.basicProjectionMatrix);
        
        deferredBuffer.fb.bindWrite(true);
        
        return PortalRenderInfo.renderAndDecideVisibility(portal, () -> {
            
            ViewAreaRenderer.renderPortalArea(
                portal, Vec3.ZERO,
                modelView,
                RenderSystem.getProjectionMatrix(),
                true, false, false, true
            );
        });
    }
    
    @Override
    public void onBeforeHandRendering(Matrix4f modelView) {
        if (PortalRendering.isRendering()) {
            return;
        }
        
        CHelper.checkGlError();
        
        // save the main framebuffer to deferredBuffer
        IPIrisHelper.newCopyDepthStencil(
            client.getMainRenderTarget(),
            deferredBuffer.fb
        );
        IPIrisHelper.copyColor(
            client.getMainRenderTarget(),
            deferredBuffer.fb
        );
//        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, client.getMainRenderTarget().frameBufferId);
//        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, deferredBuffer.fb.frameBufferId);
//        GL30.glBlitFramebuffer(
//            0, 0, deferredBuffer.fb.width, deferredBuffer.fb.height,
//            0, 0, deferredBuffer.fb.width, deferredBuffer.fb.height,
//            GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT,
//            GL_NEAREST
//        );
        
        CHelper.checkGlError();
        
        renderPortals(passingModelView);
        
        RenderTarget mainFrameBuffer = client.getMainRenderTarget();
        mainFrameBuffer.bindWrite(true);
        
        MyRenderHelper.drawScreenFrameBuffer(
            deferredBuffer.fb,
            false,
            false
        );
    }
    
    @Override
    public void onHandRenderingEnded() {
    
    }
    
    protected void renderPortals(Matrix4f modelView) {
        List<Portal> portalsToRender = getPortalsToRender(modelView);
        
        for (Portal portal : portalsToRender) {
            doRenderPortal(portal, modelView);
        }
    }
}
