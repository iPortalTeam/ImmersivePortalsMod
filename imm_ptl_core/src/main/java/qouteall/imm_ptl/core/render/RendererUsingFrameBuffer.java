package qouteall.imm_ptl.core.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.GL11;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.compat.IPPortingLibCompat;
import qouteall.imm_ptl.core.ducks.IEMinecraftClient;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalLike;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;

import java.util.List;

public class RendererUsingFrameBuffer extends PortalRenderer {
    SecondaryFrameBuffer secondaryFrameBuffer = new SecondaryFrameBuffer();
    
    @Override
    public void onBeforeTranslucentRendering(PoseStack matrixStack) {
        renderPortals(matrixStack);
    }
    
    @Override
    public void onAfterTranslucentRendering(PoseStack matrixStack) {
    
    }
    
    @Override
    public void onHandRenderingEnded(PoseStack matrixStack) {
    
    }
    
    @Override
    public void finishRendering() {
    
    }
    
    @Override
    public void prepareRendering() {
        secondaryFrameBuffer.prepare();
        
        GlStateManager._enableDepthTest();
        
        GL11.glDisable(GL11.GL_STENCIL_TEST);
    
        IPPortingLibCompat.setIsStencilEnabled(client.getMainRenderTarget(), false);
//        ((IEFrameBuffer) client.getMainRenderTarget()).setIsStencilBufferEnabledAndReload(false);
    }
    
    protected void doRenderPortal(
        PortalLike portal,
        PoseStack matrixStack
    ) {
        if (PortalRendering.isRendering()) {
            //only support one-layer portal
            return;
        }
        
        if (!testShouldRenderPortal(portal, matrixStack)) {
            return;
        }
        
        PortalRendering.pushPortalLayer(portal);
        
        RenderTarget oldFrameBuffer = client.getMainRenderTarget();
        
        ((IEMinecraftClient) client).setFrameBuffer(secondaryFrameBuffer.fb);
        secondaryFrameBuffer.fb.bindWrite(true);
        
        GlStateManager._clearColor(1, 0, 1, 1);
        GlStateManager._clearDepth(1);
        GlStateManager._clear(
            GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT,
            Minecraft.ON_OSX
        );
        GL11.glDisable(GL11.GL_STENCIL_TEST);
        
        renderPortalContent(portal);
        
        ((IEMinecraftClient) client).setFrameBuffer(oldFrameBuffer);
        oldFrameBuffer.bindWrite(true);
        
        PortalRendering.popPortalLayer();
        
        CHelper.enableDepthClamp();
        renderSecondBufferIntoMainBuffer(portal, matrixStack);
        CHelper.disableDepthClamp();
        
        MyRenderHelper.debugFramebufferDepth();
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
        PoseStack matrixStack
    ) {
        FrontClipping.updateInnerClipping(matrixStack);
        return QueryManager.renderAndGetDoesAnySamplePass(() -> {
            ViewAreaRenderer.renderPortalArea(
                portal, Vec3.ZERO,
                matrixStack.last().pose(),
                RenderSystem.getProjectionMatrix(),
                true, true,
                true);
        });
    }
    
    private void renderSecondBufferIntoMainBuffer(PortalLike portal, PoseStack matrixStack) {
        MyRenderHelper.drawPortalAreaWithFramebuffer(
            portal,
            secondaryFrameBuffer.fb,
            matrixStack.last().pose(),
            RenderSystem.getProjectionMatrix()
        );
    }
    
    protected void renderPortals(PoseStack matrixStack) {
        List<PortalLike> portalsToRender = getPortalsToRender(matrixStack);
    
        for (PortalLike portal : portalsToRender) {
            doRenderPortal(portal, matrixStack);
        }
    }
}
