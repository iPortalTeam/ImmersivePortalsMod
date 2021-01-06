package com.qouteall.immersive_portals.render;

import com.qouteall.immersive_portals.CHelper;
import com.qouteall.immersive_portals.portal.PortalLike;
import com.qouteall.immersive_portals.portal.nether_portal.BlockPortalShape;
import com.qouteall.immersive_portals.portal.nether_portal.BreakablePortalEntity;
import com.qouteall.immersive_portals.render.context_management.RenderStates;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.Random;

@Environment(EnvType.CLIENT)
public class OverlayRendering {
    private static final Random random = new Random();
    
    private static final VertexConsumerProvider.Immediate vertexConsumerProvider =
        VertexConsumerProvider.immediate(new BufferBuilder(256));
    
    public static class PortalOverlayRenderLayer extends RenderLayer {
        
        public PortalOverlayRenderLayer() {
            super(
                "imm_ptl_portal_overlay",
                RenderLayer.getTranslucentMovingBlock().getVertexFormat(),
                RenderLayer.getTranslucentMovingBlock().getDrawMode(),
                RenderLayer.getTranslucentMovingBlock().getExpectedBufferSize(),
                RenderLayer.getTranslucentMovingBlock().hasCrumbling(),
                true,
                () -> {
                    RenderLayer.getTranslucentMovingBlock().startDrawing();
                },
                () -> {
                    RenderLayer.getTranslucentMovingBlock().endDrawing();
                }
            );
        }
    }
    
    public static final PortalOverlayRenderLayer portalOverlayRenderLayer = new PortalOverlayRenderLayer();
    
    public static void onPortalRendered(
        PortalLike portal,
        MatrixStack matrixStack
    ) {
        if (portal instanceof BreakablePortalEntity) {
            renderBreakablePortalOverlay(
                ((BreakablePortalEntity) portal),
                RenderStates.tickDelta,
                matrixStack,
                vertexConsumerProvider
            );
        }
    }
    
    /**
     * {@link net.minecraft.client.render.entity.FallingBlockEntityRenderer}
     */
    private static void renderBreakablePortalOverlay(
        BreakablePortalEntity portal,
        float tickDelta,
        MatrixStack matrixStack,
        VertexConsumerProvider.Immediate vertexConsumerProvider
    ) {
        BlockState blockState = portal.overlayBlockState;
        
        Vec3d cameraPos = CHelper.getCurrentCameraPos();
        
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
        
        matrixStack.translate(pos.x - cameraPos.x, pos.y - cameraPos.y, pos.z - cameraPos.z);
        
        BakedModel model = blockRenderManager.getModel(blockState);
        PortalOverlayRenderLayer renderLayer = OverlayRendering.portalOverlayRenderLayer;
        VertexConsumer buffer = vertexConsumerProvider.getBuffer(renderLayer);
        
        Direction facing =
            Direction.getFacing(portal.getNormal().x, portal.getNormal().y, portal.getNormal().z);
        
        List<BakedQuad> quads = model.getQuads(blockState, facing, random);
        BakedQuad quad = quads.get(0);
        
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
        
        matrixStack.pop();
        
        vertexConsumerProvider.draw(renderLayer);
        
    }
}
