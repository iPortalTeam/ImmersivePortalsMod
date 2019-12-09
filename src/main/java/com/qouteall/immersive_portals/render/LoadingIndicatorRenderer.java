package com.qouteall.immersive_portals.render;

import com.qouteall.immersive_portals.portal.LoadingIndicatorEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class LoadingIndicatorRenderer extends EntityRenderer<LoadingIndicatorEntity> {
    public LoadingIndicatorRenderer(EntityRenderDispatcher entityRenderDispatcher_1) {
        super(entityRenderDispatcher_1);
    }
    
    @Override
    public Identifier getTexture(LoadingIndicatorEntity var1) {
        return null;
    }
    
    @Override
    public void render(
        LoadingIndicatorEntity entity_1,
        float float_1,
        float float_2,
        MatrixStack matrixStack_1,
        VertexConsumerProvider vertexConsumerProvider_1,
        int int_1
    ) {
        String[] splited = entity_1.getText().split("\n");
        for (int i = 0; i < splited.length; i++) {
            matrixStack_1.push();
            matrixStack_1.translate(0, -i * 0.5, 0);
            this.renderLabelIfPresent(
                entity_1,
                splited[i],
                matrixStack_1,
                vertexConsumerProvider_1,
                int_1
            );
            matrixStack_1.pop();
        }
    }
}
