package com.qouteall.immersive_portals.render;

import com.qouteall.immersive_portals.portal.Portal;

public class RendererDummy extends PortalRenderer {
    @Override
    public boolean shouldSkipClearing() {
        return false;
    }
    
    @Override
    protected void prepareStates() {
    
    }
    
    @Override
    protected void doRenderPortal(Portal portal) {
    
    }
    
    @Override
    public void renderPortalInEntityRenderer(Portal portal) {
    
    }
}
