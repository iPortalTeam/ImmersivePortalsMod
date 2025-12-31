package qouteall.imm_ptl.core.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import qouteall.imm_ptl.core.DirectionHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.compat.IrisCompat;
import qouteall.imm_ptl.core.compat.sodium_compatibility.SodiumInterface;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.nether_portal.BlockPortalShape;
import qouteall.imm_ptl.core.portal.nether_portal.BreakablePortalEntity;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;
import qouteall.imm_ptl.core.render.context_management.RenderStates;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class OverlayRendering {
    private static final RandomSource random = RandomSource.create();
    
    
    public static boolean shouldRenderOverlay(Portal portal) {
        if (portal instanceof BreakablePortalEntity breakablePortalEntity) {
            if (breakablePortalEntity.getActualOverlay() != null) {
                return breakablePortalEntity.isInFrontOfPortal(CHelper.getCurrentCameraPos());
            }
        }
        return false;
    }
    
    private static boolean shaderOverlayWarned = false;
    
    public static void onRenderPortalEntity(
        Portal portal,
        PoseStack matrixStack,
        MultiBufferSource vertexConsumerProvider
    ) {
        if (IrisCompat.isShaders()) {
            if (!shaderOverlayWarned) {
                shaderOverlayWarned = true;
                CHelper.printChat("[Immersive Portals] Portal overlay cannot be rendered with shaders");
            }
            
            return;
        }
        
        if (portal instanceof BreakablePortalEntity) {
            renderBreakablePortalOverlay(
                ((BreakablePortalEntity) portal),
                RenderStates.getPartialTick(),
                matrixStack,
                vertexConsumerProvider
            );
        }
    }

    public static void submitPortalOverlay(
        Portal portal,
        PoseStack matrixStack,
        SubmitNodeCollector collector
    ) {
        if (IrisCompat.isShaders()) {
            if (!shaderOverlayWarned) {
                shaderOverlayWarned = true;
                CHelper.printChat("[Immersive Portals] Portal overlay cannot be rendered with shaders");
            }
            return;
        }
        
        if (portal instanceof BreakablePortalEntity breakablePortal) {
            submitBreakablePortalOverlay(
                breakablePortal,
                RenderStates.getPartialTick(),
                matrixStack,
                collector
            );
        }
    }
    
    public static List<BakedQuad> getQuads(BlockStateModel model, BlockState blockState, Vec3 portalNormal) {
        Direction facing = DirectionHelper.nearestDirection(portalNormal.x, portalNormal.y, portalNormal.z);
        
        List<BakedQuad> result = new ArrayList<>();
        
        List<BlockModelPart> parts = model.collectParts(random);
        for (BlockModelPart part : parts) {
            result.addAll(part.getQuads(facing));
        }
        
        if (result.isEmpty()) {
            for (Direction direction : Direction.values()) {
                for (BlockModelPart part : parts) {
                    result.addAll(part.getQuads(direction));
                }
            }
        }
        
        return result;
    }
    
    /**
     * {@link net.minecraft.client.renderer.entity.FallingBlockRenderer}
     */
    private static void renderBreakablePortalOverlay(
        BreakablePortalEntity portal,
        float partialTick,
        PoseStack matrixStack,
        MultiBufferSource vertexConsumerProvider
    ) {
//        if (PortalRendering.isRendering()) {
//            return;
//        }
        
        BreakablePortalEntity.OverlayInfo overlay = portal.getActualOverlay();
        
        if (overlay == null) {
            return;
        }
        
        BlockState blockState = overlay.blockState();
        
        Vec3 cameraPos = CHelper.getCurrentCameraPos();
        
        if (blockState == null) {
            return;
        }
        
        BlockRenderDispatcher blockRenderManager = Minecraft.getInstance().getBlockRenderer();
        
        BlockPortalShape blockPortalShape = portal.blockPortalShape;
        if (blockPortalShape == null) {
            return;
        }
        
        matrixStack.pushPose();
        
        Vec3 offset = portal.getNormal().scale(overlay.offset());
        
        Vec3 pos = portal.position();
        
        matrixStack.translate(offset.x, offset.y, offset.z);
        
        BlockStateModel model = blockRenderManager.getBlockModel(blockState);
        RenderType renderLayer = Sheets.translucentBlockItemSheet();
        VertexConsumer buffer = vertexConsumerProvider.getBuffer(renderLayer);
        
        renderBreakablePortalOverlayWithConsumer(
            portal,
            overlay,
            blockPortalShape,
            model,
            buffer,
            matrixStack
        );
    }

    private static void submitBreakablePortalOverlay(
        BreakablePortalEntity portal,
        float partialTick,
        PoseStack matrixStack,
        SubmitNodeCollector collector
    ) {
        BreakablePortalEntity.OverlayInfo overlay = portal.getActualOverlay();
        
        if (overlay == null) {
            return;
        }
        
        BlockState blockState = overlay.blockState();
        if (blockState == null) {
            return;
        }
        
        BlockPortalShape blockPortalShape = portal.blockPortalShape;
        if (blockPortalShape == null) {
            return;
        }
        
        BlockRenderDispatcher blockRenderManager = Minecraft.getInstance().getBlockRenderer();
        BlockStateModel model = blockRenderManager.getBlockModel(blockState);
        RenderType renderLayer = Sheets.translucentBlockItemSheet();
        
        collector.submitCustomGeometry(
            matrixStack,
            renderLayer,
            (pose, vertexConsumer) -> {
                PoseStack tempStack = new PoseStack();
                tempStack.last().pose().set(pose.pose());
                tempStack.last().normal().set(pose.normal());
                renderBreakablePortalOverlayWithConsumer(
                    portal,
                    overlay,
                    blockPortalShape,
                    model,
                    vertexConsumer,
                    tempStack
                );
            }
        );
    }
    
    private static void renderBreakablePortalOverlayWithConsumer(
        BreakablePortalEntity portal,
        BreakablePortalEntity.OverlayInfo overlay,
        BlockPortalShape blockPortalShape,
        BlockStateModel model,
        VertexConsumer buffer,
        PoseStack matrixStack
    ) {
        Vec3 offset = portal.getNormal().scale(overlay.offset());
        Vec3 pos = portal.position();
        
        matrixStack.pushPose();
        
        matrixStack.translate(offset.x, offset.y, offset.z);
        
        List<BakedQuad> quads = getQuads(model, overlay.blockState(), portal.getNormal());
        
        random.setSeed(0);
        
        for (BlockPos blockPos : blockPortalShape.area) {
            matrixStack.pushPose();
            matrixStack.translate(
                blockPos.getX() - pos.x, blockPos.getY() - pos.y, blockPos.getZ() - pos.z
            );
            
            if (overlay.rotation() != null) {
                matrixStack.mulPose(overlay.rotation().toMcQuaternion());
            }
            
            for (BakedQuad quad : quads) {
                SodiumInterface.invoker.markSpriteActive(quad.sprite());
                buffer.putBulkData(
                    matrixStack.last(),
                    quad,
                    new float[]{1.0F, 1.0F, 1.0F, 1.0F},
                    1.0f, 1.0f, 1.0f, (float) overlay.opacity(),
                    new int[]{14680304, 14680304, 14680304, 14680304},//packed light value
                    OverlayTexture.NO_OVERLAY
                );
            }
            
            matrixStack.popPose();
        }
        
        matrixStack.popPose();
    }
}
