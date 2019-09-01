package com.qouteall.immersive_portals.render;

import com.qouteall.immersive_portals.portal.Portal;

public class RendererDummy extends PortalRenderer {
    @Override
    public boolean shouldSkipClearing() {
        return false;
    }
    
    @Override
    public void prepareRendering() {
    
    }
    
    @Override
    public void onBeforeTranslucentRendering() {
    
    }
    
    @Override
    public void onAfterTranslucentRendering() {
    
    }
    
    @Override
    public void onRenderCenterEnded() {
    
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
