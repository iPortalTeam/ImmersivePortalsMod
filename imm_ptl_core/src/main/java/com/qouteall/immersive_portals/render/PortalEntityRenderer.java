package com.qouteall.immersive_portals.render;

import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.portal.nether_portal.BreakablePortalEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class PortalEntityRenderer extends EntityRenderer<Portal> {
    public PortalEntityRenderer(EntityRenderDispatcher entityRenderDispatcher_1) {
        super(entityRenderDispatcher_1);
    }
    
    @Override
    public void render(
        Portal portal,
        float yaw,
        float tickDelta,
        MatrixStack matrixStack,
        VertexConsumerProvider vertexConsumerProvider,
        int light
    ) {
        super.render(portal, yaw, tickDelta, matrixStack, vertexConsumerProvider, light);
        CGlobal.renderer.renderPortalInEntityRenderer(portal);
        
        if (portal instanceof BreakablePortalEntity) {
            BreakablePortalEntity breakablePortalEntity = (BreakablePortalEntity) portal;
            renderBreakablePortalOverlay(
                breakablePortalEntity, tickDelta, matrixStack, vertexConsumerProvider, light
            );
        }
    }
    
    @Override
    public Identifier getTexture(Portal var1) {
        return null;
    }
    
    
    private void renderBreakablePortalOverlay(
        BreakablePortalEntity portal,
        float tickDelta,
        MatrixStack matrixStack,
        VertexConsumerProvider vertexConsumerProvider,
        int light
    ) {
//        String overlayTextureId = portal.overlayTextureId;
//
//        if (overlayTextureId == null) {
//            return;
//        }
//
//        if (portal.overlayRenderingModel == null) {
//            BlockRenderManager blockRenderManager = MinecraftClient.getInstance().getBlockRenderManager();
//
//
//        }
    }
}
