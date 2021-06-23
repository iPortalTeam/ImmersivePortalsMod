package qouteall.imm_ptl.core.render;

import com.mojang.blaze3d.platform.GlStateManager;
import qouteall.imm_ptl.core.ducks.IEMinecraftClient;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalLike;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;
import qouteall.imm_ptl.core.render.context_management.RenderStates;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;

public class RendererUsingFrameBuffer extends PortalRenderer {
    SecondaryFrameBuffer secondaryFrameBuffer = new SecondaryFrameBuffer();
    
    @Override
    public void onBeforeTranslucentRendering(MatrixStack matrixStack) {
        renderPortals(matrixStack);
    }
    
    @Override
    public void onAfterTranslucentRendering(MatrixStack matrixStack) {
    
    }
    
    @Override
    public void onRenderCenterEnded(MatrixStack matrixStack) {
    
    }
    
    @Override
    public void finishRendering() {
    
    }
    
    @Override
    public void prepareRendering() {
        secondaryFrameBuffer.prepare();
        
        GlStateManager._enableDepthTest();
        
        GL11.glDisable(GL11.GL_STENCIL_TEST);
    }
    
    @Override
    protected void doRenderPortal(
        PortalLike portal,
        MatrixStack matrixStack
    ) {
        if (PortalRendering.isRendering()) {
            //only support one-layer portal
            return;
        }
        
        if (!testShouldRenderPortal(portal, matrixStack)) {
            return;
        }
        
        PortalRendering.pushPortalLayer(portal);
        
        Framebuffer oldFrameBuffer = client.getFramebuffer();
        
        ((IEMinecraftClient) client).setFrameBuffer(secondaryFrameBuffer.fb);
        secondaryFrameBuffer.fb.beginWrite(true);
        
        GlStateManager._clearColor(1, 0, 1, 1);
        GlStateManager._clearDepth(1);
        GlStateManager._clear(
            GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT,
            MinecraftClient.IS_SYSTEM_MAC
        );
        GL11.glDisable(GL11.GL_STENCIL_TEST);
        
        renderPortalContent(portal);
        
        ((IEMinecraftClient) client).setFrameBuffer(oldFrameBuffer);
        oldFrameBuffer.beginWrite(true);
        
        PortalRendering.popPortalLayer();
        
        renderSecondBufferIntoMainBuffer(portal, matrixStack);
    }
    
    @Override
    public void renderPortalInEntityRenderer(Portal portal) {
        //nothing
    }
    
    @Override
    public boolean replaceFrameBufferClearing() {
        return false;
    }
    
    private boolean testShouldRenderPortal(
        PortalLike portal,
        MatrixStack matrixStack
    ) {
        FrontClipping.updateInnerClipping();
        return QueryManager.renderAndGetDoesAnySamplePass(() -> {
            ViewAreaRenderer.renderPortalArea(
                portal, Vec3d.ZERO,
                matrixStack.peek().getModel(),
                RenderStates.projectionMatrix,
                true, true,
                true);
        });
    }
    
    private void renderSecondBufferIntoMainBuffer(PortalLike portal, MatrixStack matrixStack) {
        MyRenderHelper.drawPortalAreaWithFramebuffer(
            portal,
            secondaryFrameBuffer.fb,
            matrixStack.peek().getModel(),
            RenderStates.projectionMatrix
        );
    }
    
}
