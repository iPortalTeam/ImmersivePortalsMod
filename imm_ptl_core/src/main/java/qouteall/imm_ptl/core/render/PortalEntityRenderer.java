package qouteall.imm_ptl.core.render;

import qouteall.imm_ptl.core.CGlobal;
import qouteall.imm_ptl.core.portal.Portal;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class PortalEntityRenderer extends EntityRenderer<Portal> {
    
    public PortalEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
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
        
//        if (portal instanceof BreakablePortalEntity) {
//            BreakablePortalEntity breakablePortalEntity = (BreakablePortalEntity) portal;
//            OverlayRendering.renderBreakablePortalOverlay(
//                breakablePortalEntity, tickDelta, matrixStack, vertexConsumerProvider, light
//            );
//        }
//
        super.render(portal, yaw, tickDelta, matrixStack, vertexConsumerProvider, light);
    }
    
    @Override
    public Identifier getTexture(Portal portal) {
//        if (portal instanceof BreakablePortalEntity) {
//            if (((BreakablePortalEntity) portal).overlayBlockState != null) {
//                return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
//            }
//        }
        return null;
    }
    
}
