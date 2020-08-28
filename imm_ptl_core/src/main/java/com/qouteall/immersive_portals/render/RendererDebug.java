package com.qouteall.immersive_portals.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.render.context_management.PortalRendering;
import com.qouteall.immersive_portals.render.context_management.RenderStates;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.opengl.GL11;

public class RendererDebug extends PortalRenderer {
    @Override
    public void onBeforeTranslucentRendering(MatrixStack matrixStack) {
    
    }
    
    @Override
    public void onAfterTranslucentRendering(MatrixStack matrixStack) {
        renderPortals(matrixStack);
    }
    
    @Override
    public void onRenderCenterEnded(MatrixStack matrixStack) {
    
    }
    
    @Override
    public void prepareRendering() {
    
    }
    
    @Override
    public void finishRendering() {
    
    }
    
    @Override
    public void renderPortalInEntityRenderer(Portal portal) {
    
    }
    
    @Override
    public boolean shouldSkipClearing() {
        return false;
    }
    
    @Override
    protected void doRenderPortal(Portal portal, MatrixStack matrixStack) {
        if (RenderStates.getRenderedPortalNum() != 0) {
            return;
        }
        
        if (!testShouldRenderPortal(portal, matrixStack)) {
            return;
        }
    
        PortalRendering.pushPortalLayer(portal);
        
        GlStateManager.clearColor(1, 0, 1, 1);
        GlStateManager.clearDepth(1);
        GlStateManager.clear(
            GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT,
            MinecraftClient.IS_SYSTEM_MAC
        );
        GL11.glDisable(GL11.GL_STENCIL_TEST);
        
        renderPortalContent(portal);
    
        PortalRendering.popPortalLayer();
    }
    
    private boolean testShouldRenderPortal(
        Portal portal,
        MatrixStack matrixStack
    ) {
        return QueryManager.renderAndGetDoesAnySamplePassed(() -> {
            GlStateManager.enableDepthTest();
            GlStateManager.depthMask(false);
            //GL20.glUseProgram(0);
            ViewAreaRenderer.drawPortalViewTriangle(portal, matrixStack, false, true);
            GlStateManager.depthMask(true);
        });
    }
}
