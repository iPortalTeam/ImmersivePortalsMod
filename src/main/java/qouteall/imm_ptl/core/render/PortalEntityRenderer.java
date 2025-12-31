package qouteall.imm_ptl.core.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import qouteall.imm_ptl.core.IPCGlobal;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.mc_utils.WireRenderingHelper;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;

@Environment(EnvType.CLIENT)
public class PortalEntityRenderer extends EntityRenderer<Portal, PortalEntityRenderer.PortalRenderState> {

    public static class PortalRenderState extends EntityRenderState {
        public Portal portal;
    }

    public PortalEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public PortalRenderState createRenderState() {
        return new PortalRenderState();
    }

    @Override
    public void extractRenderState(Portal portal, PortalRenderState state, float partialTick) {
        state.portal = portal;
        super.extractRenderState(portal, state, partialTick);
    }

    @Override
    public void submit(
        PortalRenderState state,
        PoseStack matrixStack,
        SubmitNodeCollector collector,
        CameraRenderState cameraRenderState
    ) {
        Portal portal = state.portal;

        IPCGlobal.renderer.renderPortalInEntityRenderer(portal);

        if (OverlayRendering.shouldRenderOverlay(portal)) {
            OverlayRendering.submitPortalOverlay(portal, matrixStack, collector);
        }

        if (IPGlobal.debugRenderPortalShapeMesh && !PortalRendering.isRendering()) {
            collector.submitCustomGeometry(
                matrixStack,
                RenderTypes.lines(),
                (pose, vertexConsumer) -> WireRenderingHelper.renderPortalShapeMeshDebug(
                    pose, vertexConsumer, portal
                )
            );
        }
    }
}
