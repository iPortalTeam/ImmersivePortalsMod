package com.qouteall.immersive_portals.render;

import com.qouteall.immersive_portals.portal.Portal;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

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
        Entity cameraEntity = MinecraftClient.getInstance().cameraEntity;
        if (cameraEntity != null) {
            Vec3d cameraPos = cameraEntity.getPos();
            if (portal.isInFrontOfPortal(cameraPos)) {
                drawPortalViewTriangle(portal);
            }
        }
    }
}
