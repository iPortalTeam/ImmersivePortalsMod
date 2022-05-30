package qouteall.imm_ptl.core.compat.iris_compatibility;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.compat.IPPortingLibCompat;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalLike;
import qouteall.imm_ptl.core.portal.PortalRenderInfo;
import qouteall.imm_ptl.core.render.MyGameRenderer;
import qouteall.imm_ptl.core.render.MyRenderHelper;
import qouteall.imm_ptl.core.render.PortalRenderer;
import qouteall.imm_ptl.core.render.SecondaryFrameBuffer;
import qouteall.imm_ptl.core.render.ViewAreaRenderer;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;
import qouteall.imm_ptl.core.render.context_management.WorldRenderInfo;

import java.util.List;

import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL11.GL_STENCIL_TEST;

public class IrisCompatibilityPortalRenderer extends PortalRenderer {
    
    public static final IrisCompatibilityPortalRenderer instance = new IrisCompatibilityPortalRenderer(false);
    public static final IrisCompatibilityPortalRenderer debugModeInstance =
        new IrisCompatibilityPortalRenderer(true);
    
    private SecondaryFrameBuffer deferredBuffer = new SecondaryFrameBuffer();
    private PoseStack modelView = new PoseStack();
    
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
    public void onBeforeTranslucentRendering(PoseStack matrixStack) {
        if (PortalRendering.isRendering()) {
            return;
        }
        
        modelView = new PoseStack();
        modelView.pushPose();
        modelView.last().pose().multiply(matrixStack.last().pose());
        modelView.last().normal().mul(matrixStack.last().normal());
        
        GL11.glDisable(GL_STENCIL_TEST);
    }
    
    @Override
    public void onAfterTranslucentRendering(PoseStack matrixStack) {
    
    }
    
    @Override
    public void finishRendering() {
        GL11.glDisable(GL_STENCIL_TEST);
    }
    
    @Override
    public void prepareRendering() {
        deferredBuffer.prepare();
        
        deferredBuffer.fb.setClearColor(1, 0, 0, 0);
        deferredBuffer.fb.clear(Minecraft.ON_OSX);
        
        IPPortingLibCompat.setIsStencilEnabled(
            client.getMainRenderTarget(), false
        );
        
        // Iris now use vanilla framebuffer's depth
        client.getMainRenderTarget().bindWrite(false);
    }
    
    protected void doRenderPortal(PortalLike portal, PoseStack matrixStack) {
        if (PortalRendering.isRendering()) {
            // this renderer only supports one-layer portal
            return;
        }
        
        if (!testShouldRenderPortal(portal, matrixStack)) {
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
                matrixStack.last().pose(),
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
    
    private boolean testShouldRenderPortal(PortalLike portal, PoseStack matrixStack) {
        
        //reset projection matrix
//        client.gameRenderer.loadProjectionMatrix(RenderStates.basicProjectionMatrix);
        
        deferredBuffer.fb.bindWrite(true);
        
        return PortalRenderInfo.renderAndDecideVisibility(portal, () -> {
            
            ViewAreaRenderer.renderPortalArea(
                portal, Vec3.ZERO,
                matrixStack.last().pose(),
                RenderSystem.getProjectionMatrix(),
                true, false, false
            );
        });
    }
    
    @Override
    public void onBeforeHandRendering(PoseStack matrixStack) {
        if (PortalRendering.isRendering()) {
            return;
        }
        
        CHelper.checkGlError();
        
        // save the main framebuffer to deferredBuffer
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, client.getMainRenderTarget().frameBufferId);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, deferredBuffer.fb.frameBufferId);
        GL30.glBlitFramebuffer(
            0, 0, deferredBuffer.fb.width, deferredBuffer.fb.height,
            0, 0, deferredBuffer.fb.width, deferredBuffer.fb.height,
            GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT,
            GL_NEAREST
        );
        
        CHelper.checkGlError();
        
        renderPortals(modelView);
        modelView.popPose();
        
        RenderTarget mainFrameBuffer = client.getMainRenderTarget();
        mainFrameBuffer.bindWrite(true);
        
        MyRenderHelper.drawScreenFrameBuffer(
            deferredBuffer.fb,
            false,
            false
        );
    }
    
    @Override
    public void onHandRenderingEnded(PoseStack matrixStack) {
    
    }
    
    protected void renderPortals(PoseStack matrixStack) {
        List<PortalLike> portalsToRender = getPortalsToRender(matrixStack);
    
        for (PortalLike portal : portalsToRender) {
            doRenderPortal(portal, matrixStack);
        }
    }
}
