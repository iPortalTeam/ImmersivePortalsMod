package com.qouteall.immersive_portals.render;

import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.CHelper;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.portal.nether_portal.BlockPortalShape;
import com.qouteall.immersive_portals.portal.nether_portal.BreakablePortalEntity;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

public class PortalEntityRenderer extends EntityRenderer<Portal> {
    
    private static final Random random = new Random();
    
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
        
        CGlobal.renderer.renderPortalInEntityRenderer(portal);
        
        if (portal instanceof BreakablePortalEntity) {
            BreakablePortalEntity breakablePortalEntity = (BreakablePortalEntity) portal;
            renderBreakablePortalOverlay(
                breakablePortalEntity, tickDelta, matrixStack, vertexConsumerProvider, light
            );
        }
        
        super.render(portal, yaw, tickDelta, matrixStack, vertexConsumerProvider, light);
    }
    
    @Override
    public Identifier getTexture(Portal portal) {
        // todo is it necessary?
        if (portal instanceof BreakablePortalEntity) {
            if (((BreakablePortalEntity) portal).overlayBlockState != null) {
                return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
            }
        }
        return null;
    }
    
    /**
     * {@link net.minecraft.client.render.entity.FallingBlockEntityRenderer}
     */
    private void renderBreakablePortalOverlay(
        BreakablePortalEntity portal,
        float tickDelta,
        MatrixStack matrixStack,
        VertexConsumerProvider vertexConsumerProvider,
        int light
    ) {
        BlockState blockState = portal.overlayBlockState;
        
        if (!portal.isInFrontOfPortal(CHelper.getCurrentCameraPos())) {
            return;
        }
        
        if (blockState == null) {
            return;
        }
        
        BlockRenderManager blockRenderManager = MinecraftClient.getInstance().getBlockRenderManager();
        
        BlockPortalShape blockPortalShape = portal.blockPortalShape;
        if (blockPortalShape == null) {
            return;
        }
        
        matrixStack.push();
        
        Vec3d pos = portal.getPos();
        
        BakedModel model = blockRenderManager.getModel(blockState);
        VertexConsumer buffer = vertexConsumerProvider.getBuffer(RenderLayers.getMovingBlockLayer(blockState));
        
        for (BlockPos blockPos : blockPortalShape.area) {
            matrixStack.push();
            matrixStack.translate(
                blockPos.getX() - pos.x, blockPos.getY() - pos.y, blockPos.getZ() - pos.z
            );
            blockRenderManager.getModelRenderer().render(
                portal.world,
                model,
                blockState,
                blockPos,
                matrixStack,
                buffer,
                false,
                random,
                blockState.getRenderingSeed(blockPos),
                OverlayTexture.DEFAULT_UV
            );
            matrixStack.pop();
        }
        
        ((VertexConsumerProvider.Immediate) vertexConsumerProvider).draw();
        
        matrixStack.pop();
    }
}
