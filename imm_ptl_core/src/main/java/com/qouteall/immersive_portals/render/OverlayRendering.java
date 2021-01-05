package com.qouteall.immersive_portals.render;

import com.qouteall.immersive_portals.CHelper;
import com.qouteall.immersive_portals.portal.nether_portal.BlockPortalShape;
import com.qouteall.immersive_portals.portal.nether_portal.BreakablePortalEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

@Environment(EnvType.CLIENT)
public class OverlayRendering {
    private static final Random random = new Random();
    
    /**
     * {@link net.minecraft.client.render.entity.FallingBlockEntityRenderer}
     */
    public static void renderBreakablePortalOverlay(
        BreakablePortalEntity portal,
        float tickDelta,
        MatrixStack matrixStack,
        VertexConsumerProvider vertexConsumerProvider,
        int light
    ) {
        BlockState blockState = portal.overlayBlockState;
    
        Vec3d cameraPos = CHelper.getCurrentCameraPos();
        if (!portal.isInFrontOfPortal(cameraPos)) {
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
    
        matrixStack.translate(pos.x - cameraPos.x, pos.y - cameraPos.y, pos.z - cameraPos.z);
        
        BakedModel model = blockRenderManager.getModel(blockState);
        RenderLayer movingBlockLayer = RenderLayers.getMovingBlockLayer(blockState);
        VertexConsumer buffer = vertexConsumerProvider.getBuffer(movingBlockLayer);
        
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
        
//        ((VertexConsumerProvider.Immediate) vertexConsumerProvider).draw();
        
        
        matrixStack.pop();
    }
}
