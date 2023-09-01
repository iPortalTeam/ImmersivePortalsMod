package qouteall.imm_ptl.core.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import qouteall.imm_ptl.core.IPCGlobal;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.mc_utils.WireRenderingHelper;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;

@Environment(EnvType.CLIENT)
public class PortalEntityRenderer extends EntityRenderer<Portal> {
    
    public PortalEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }
    
    @Override
    public void render(
        Portal portal,
        float yaw,
        float tickDelta,
        PoseStack matrixStack,
        MultiBufferSource bufferSource,
        int light
    ) {
        
        IPCGlobal.renderer.renderPortalInEntityRenderer(portal);
        
        if (OverlayRendering.shouldRenderOverlay(portal)) {
            OverlayRendering.onRenderPortalEntity(portal, matrixStack, bufferSource);
        }
    
        if (IPGlobal.debugRenderPortalShapeMesh && !PortalRendering.isRendering()) {
            VertexConsumer lineVertexConsumer = bufferSource.getBuffer(RenderType.lines());
            WireRenderingHelper.renderPortalShapeMeshDebug(
                matrixStack, lineVertexConsumer, portal
            );
        }
        
        super.render(portal, yaw, tickDelta, matrixStack, bufferSource, light);
    }
    
    @Override
    public ResourceLocation getTextureLocation(Portal portal) {
//        if (portal instanceof BreakablePortalEntity) {
//            if (((BreakablePortalEntity) portal).overlayBlockState != null) {
//                return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
//            }
//        }
        return null;
    }
    
    
}
