package com.qouteall.immersive_portals.optifine_compatibility;

import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.render.PortalRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GlFramebuffer;

public class RenderDeferred extends PortalRenderer {
    GlFramebuffer deferredBuffer;
    
    @Override
    public boolean shouldSkipClearing() {
        return false;
    }
    
    @Override
    protected void prepareStates() {
        GlFramebuffer mainFrameBuffer = mc.getFramebuffer();
        if (deferredBuffer == null) {
            deferredBuffer = new GlFramebuffer(
                mainFrameBuffer.viewWidth, mainFrameBuffer.viewHeight,
                false,//no depth attachment
                MinecraftClient.IS_SYSTEM_MAC
            );
        }
    }
    
    @Override
    protected void doRenderPortal(Portal portal) {
    
    }
    
    @Override
    public void renderPortalInEntityRenderer(Portal portal) {
    
    }
}
