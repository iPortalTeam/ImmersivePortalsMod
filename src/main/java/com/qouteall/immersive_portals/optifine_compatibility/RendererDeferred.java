package com.qouteall.immersive_portals.optifine_compatibility;

import com.mojang.blaze3d.platform.GlStateManager;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.OFInterface;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.render.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GlFramebuffer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.optifine.shaders.Shaders;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

public class RendererDeferred extends PortalRenderer {
    SecondaryFrameBuffer deferredBuffer = new SecondaryFrameBuffer();
    
    @Override
    public boolean shouldSkipClearing() {
        return false;
    }
    
    @Override
    public void onBeforeTranslucentRendering(MatrixStack matrixStack) {
    
    }
    
    @Override
    public void onAfterTranslucentRendering(MatrixStack matrixStack) {
        renderPortals();
    }
    
    @Override
    public void finishRendering() {
    
    }
    
    @Override
    public void prepareRendering() {
        if (CGlobal.shaderManager == null) {
            CGlobal.shaderManager = new ShaderManager();
        }
        
        deferredBuffer.prepare();
        
        deferredBuffer.fb.setClearColor(1, 0, 0, 0);
        deferredBuffer.fb.clear(MinecraftClient.IS_SYSTEM_MAC);
    
        OFInterface.bindToShaderFrameBuffer.run();
    
    }
    
    @Override
    protected void doRenderPortal(Portal portal) {
        if (isRendering()) {
            //currently only support one-layer portal
            return;
        }
    
        OFHelper.copyFromShaderFbTo(deferredBuffer.fb, GL11.GL_DEPTH_BUFFER_BIT);
    
        if (!testShouldRenderPortal(portal)) {
            return;
        }
    
        portalLayers.push(portal);
    
        manageCameraAndRenderPortalContent(portal);
        //it will bind the gbuffer of rendered dimension
    
        portalLayers.pop();
    
        deferredBuffer.fb.beginWrite(true);
    
        RenderHelper.drawFrameBufferUp(portal, mc.getFramebuffer(), CGlobal.shaderManager);
    
        OFInterface.bindToShaderFrameBuffer.run();
    }
    
    @Override
    protected void renderPortalContentWithContextSwitched(
        Portal portal, Vec3d oldCameraPos
    ) {
        OFGlobal.shaderContextManager.switchContextAndRun(
            () -> {
                OFInterface.bindToShaderFrameBuffer.run();
                super.renderPortalContentWithContextSwitched(portal, oldCameraPos);
            }
        );
    }
    
    @Override
    public void renderPortalInEntityRenderer(Portal portal) {
        if (shouldRenderPortalInEntityRenderer(portal)) {
            ViewAreaRenderer.drawPortalViewTriangle(portal);
        }
    }
    
    private boolean shouldRenderPortalInEntityRenderer(Portal portal) {
        Entity cameraEntity = MinecraftClient.getInstance().cameraEntity;
        if (cameraEntity == null) {
            return false;
        }
        Vec3d cameraPos = cameraEntity.getPos();
        if (Shaders.isShadowPass) {
            return true;
        }
        if (isRendering()) {
            return portal.isInFrontOfPortal(cameraPos);
        }
        return false;
    }
    
    //NOTE it will write to shader depth buffer
    private boolean testShouldRenderPortal(Portal portal) {
        return QueryManager.renderAndGetDoesAnySamplePassed(() -> {
            GlStateManager.enableDepthTest();
            GlStateManager.disableTexture();
            GlStateManager.colorMask(false, false, false, false);
            RenderHelper.setupCameraTransformation();
            GL20.glUseProgram(0);
    
            ViewAreaRenderer.drawPortalViewTriangle(portal);
            
            GlStateManager.enableTexture();
            GlStateManager.colorMask(true, true, true, true);
        });
    }
    
    @Override
    public void onRenderCenterEnded(MatrixStack matrixStack) {
        if (isRendering()) {
            return;
        }
    
        if (RenderHelper.getRenderedPortalNum() == 0) {
            return;
        }
    
        GlStateManager.enableAlphaTest();
        GlFramebuffer mainFrameBuffer = mc.getFramebuffer();
        mainFrameBuffer.beginWrite(true);
    
        RenderHelper.myDrawFrameBuffer(
            deferredBuffer.fb,
            true,
            false
        );
    }
}
