package qouteall.imm_ptl.core.render;

import com.mojang.blaze3d.vertex.PoseStack;
import qouteall.imm_ptl.core.portal.Portal;

import java.util.List;

public class RendererDummy extends PortalRenderer {
    @Override
    public boolean replaceFrameBufferClearing() {
        return false;
    }
    
    @Override
    public void prepareRendering() {
    
    }
    
    @Override
    public void onBeforeTranslucentRendering(PoseStack matrixStack) {
    
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
    
    protected void doRenderPortal(
        PortalRenderable portal,
        PoseStack matrixStack
    ) {
    
    }
    
    @Override
    public void renderPortalInEntityRenderer(Portal portal) {
    
    }
    
    protected void renderPortals(PoseStack matrixStack) {
        List<PortalRenderable> portalsToRender = getPortalsToRender(matrixStack);
    
        for (PortalRenderable portal : portalsToRender) {
            doRenderPortal(portal, matrixStack);
        }
    }
}
