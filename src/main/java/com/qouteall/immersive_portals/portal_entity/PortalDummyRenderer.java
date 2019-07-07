package com.qouteall.immersive_portals.portal_entity;

import net.minecraft.client.render.VisibleRegion;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.util.Identifier;

public class PortalDummyRenderer extends EntityRenderer<PortalEntity> {
    protected PortalDummyRenderer(EntityRenderDispatcher entityRenderDispatcher_1) {
        super(entityRenderDispatcher_1);
    }
    
    @Override
    public boolean isVisible(
        PortalEntity entity_1,
        VisibleRegion visibleRegion_1,
        double double_1,
        double double_2,
        double double_3
    ) {
        return false;
    }
    
    @Override
    protected Identifier getTexture(PortalEntity var1) {
        return null;
    }
}
