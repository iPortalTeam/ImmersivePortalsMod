package com.qouteall.immersive_portals.portal;

import com.qouteall.immersive_portals.CGlobal;
import net.minecraft.client.render.VisibleRegion;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.util.Identifier;

public class PortalDummyRenderer extends EntityRenderer<Portal> {
    public PortalDummyRenderer(EntityRenderDispatcher entityRenderDispatcher_1) {
        super(entityRenderDispatcher_1);
    }
    
    @Override
    public boolean isVisible(
        Portal entity_1,
        VisibleRegion visibleRegion_1,
        double double_1,
        double double_2,
        double double_3
    ) {
        return true;
    }
    
    @Override
    public void render(
        Portal portal,
        double double_1,
        double double_2,
        double double_3,
        float float_1,
        float float_2
    ) {
        super.render(portal, double_1, double_2, double_3, float_1, float_2);
        CGlobal.renderer.renderPortalInEntityRenderer(portal);
    }
    
    @Override
    protected Identifier getTexture(Portal var1) {
        return null;
    }
}
