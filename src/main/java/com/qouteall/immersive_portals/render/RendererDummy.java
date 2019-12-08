package com.qouteall.immersive_portals.render;

import com.qouteall.immersive_portals.portal.Portal;
import net.minecraft.client.util.math.MatrixStack;

public class RendererDummy extends PortalRenderer {
    @Override
    public boolean shouldSkipClearing() {
        return false;
    }
    
    @Override
    public void prepareRendering() {
    
    }
    
    @Override
    public void onBeforeTranslucentRendering(MatrixStack matrixStack) {
    
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
    protected void doRenderPortal(Portal portal) {
    
    }
    
    @Override
    public void renderPortalInEntityRenderer(Portal portal) {
    
    }
}
