package com.qouteall.immersive_portals.portal;

import com.qouteall.immersive_portals.CGlobal;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class PortalEntityRenderer extends EntityRenderer<Portal> {
    public PortalEntityRenderer(EntityRenderDispatcher entityRenderDispatcher_1) {
        super(entityRenderDispatcher_1);
    }
    
    //
//    @Override
//    public boolean isVisible(
//        Portal entity_1,
//        VisibleRegion visibleRegion_1,
//        double double_1,
//        double double_2,
//        double double_3
//    ) {
//        return true;
//    }
//
    @Override
    public void render(
        Portal portal,
        float float_1,
        float float_2,
        MatrixStack matrixStack_1,
        VertexConsumerProvider vertexConsumerProvider_1,
        int int_1
    ) {
        super.render(portal, float_1, float_2, matrixStack_1, vertexConsumerProvider_1, int_1);
        CGlobal.renderer.renderPortalInEntityRenderer(portal);
    }
    
    @Override
    public Identifier getTexture(Portal var1) {
        return null;
    }
}
