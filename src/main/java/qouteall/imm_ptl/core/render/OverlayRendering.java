package qouteall.imm_ptl.core.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.compat.iris_compatibility.IrisInterface;
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
        if (IrisInterface.invoker.isShaders()) {
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
    
    public static List<BakedQuad> getQuads(BakedModel model, BlockState blockState, Vec3 portalNormal) {
        Direction facing = Direction.getNearest(portalNormal.x, portalNormal.y, portalNormal.z);
        
        List<BakedQuad> result = new ArrayList<>();
        
        result.addAll(model.getQuads(blockState, facing, random));
        
        result.addAll(model.getQuads(blockState, null, random));
        
        if (result.isEmpty()) {
            for (Direction direction : Direction.values()) {
                result.addAll(model.getQuads(blockState, direction, random));
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
        
        BakedModel model = blockRenderManager.getBlockModel(blockState);
        RenderType renderLayer = Sheets.translucentCullBlockSheet();
        VertexConsumer buffer = vertexConsumerProvider.getBuffer(renderLayer);
        
        List<BakedQuad> quads = getQuads(model, blockState, portal.getNormal());
        
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
                SodiumInterface.invoker.markSpriteActive(quad.getSprite());
                buffer.putBulkData(
                    matrixStack.last(),
                    quad,
                    new float[]{1.0F, 1.0F, 1.0F, 1.0F},
                    1.0f, 1.0f, 1.0f, (float) overlay.opacity(),
                    new int[]{14680304, 14680304, 14680304, 14680304},//packed light value
                    OverlayTexture.NO_OVERLAY,
                    true
                );
            }
            
            matrixStack.popPose();
        }
        
        matrixStack.popPose();
        
    }
}
