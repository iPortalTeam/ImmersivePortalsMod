package com.qouteall.immersive_portals.render;

import com.qouteall.immersive_portals.portal.LoadingIndicatorEntity;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.util.Identifier;

public class LoadingIndicatorRenderer extends EntityRenderer<LoadingIndicatorEntity> {
    public LoadingIndicatorRenderer(EntityRenderDispatcher entityRenderDispatcher_1) {
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
        String[] splited = entity_1.getText().split("\n");
        for (int i = 0; i < splited.length; i++) {
            this.renderLabel(
                entity_1, double_1, double_2 - i * 0.5, double_3,
                splited[i], 64
            );
        }
    }
}
