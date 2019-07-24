package com.qouteall.immersive_portals.portal;

import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.util.Identifier;

public class LoadingIndicatorRenderer extends EntityRenderer<LoadingIndicatorEntity> {
    protected LoadingIndicatorRenderer(EntityRenderDispatcher entityRenderDispatcher_1) {
        super(entityRenderDispatcher_1);
    }
    
    @Override
    protected Identifier getTexture(LoadingIndicatorEntity var1) {
        return null;
    }
    
    @Override
    public void render(
        LoadingIndicatorEntity entity_1,
        double double_1,
        double double_2,
        double double_3,
        float float_1,
        float float_2
    ) {
        this.renderLabel(entity_1, "Loading", double_1, double_2, double_3, 64);
    }
}
