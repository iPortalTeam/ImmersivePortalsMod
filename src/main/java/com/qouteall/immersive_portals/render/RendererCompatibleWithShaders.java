package com.qouteall.immersive_portals.render;

import com.qouteall.immersive_portals.portal.Portal;

public class RendererCompatibleWithShaders extends PortalRenderer {
    @Override
    public boolean shouldSkipClearing() {
        return false;
    }
    
    @Override
    protected void prepareStates() {
        //nothing
    }
    
    @Override
    protected void doRenderPortal(Portal portal) {
        //nothing
    }
    
    @Override
    public void renderPortalInEntityRenderer(Portal portal) {
        drawPortalViewTriangle(portal);
    }
}
