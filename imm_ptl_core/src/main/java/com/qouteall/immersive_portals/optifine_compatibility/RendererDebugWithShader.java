package com.qouteall.immersive_portals.optifine_compatibility;

import com.mojang.blaze3d.platform.GlStateManager;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.render.MyGameRenderer;
import com.qouteall.immersive_portals.render.PortalRenderer;
import com.qouteall.immersive_portals.render.SecondaryFrameBuffer;
import com.qouteall.immersive_portals.render.ShaderManager;
import com.qouteall.immersive_portals.render.context_management.PortalRendering;
import com.qouteall.immersive_portals.render.context_management.RenderInfo;
import com.qouteall.immersive_portals.render.context_management.RenderStates;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.opengl.GL13;

public class RendererDebugWithShader extends PortalRenderer {
    SecondaryFrameBuffer deferredBuffer = new SecondaryFrameBuffer();
    
    @Override
    public boolean shouldSkipClearing() {
        return false;
    }
    
    @Override
    public void renderPortalInEntityRenderer(Portal portal) {
    
    }
    
    @Override
    public void onBeforeTranslucentRendering(MatrixStack matrixStack) {
    
    }
    
    @Override
    public void onAfterTranslucentRendering(MatrixStack matrixStack) {
        renderPortals(matrixStack);
    }
    
    @Override
    public void prepareRendering() {
        if (CGlobal.shaderManager == null) {
            CGlobal.shaderManager = new ShaderManager();
        }
        
        deferredBuffer.prepare();
        
        deferredBuffer.fb.setClearColor(1, 0, 0, 0);
        deferredBuffer.fb.clear(MinecraftClient.IS_SYSTEM_MAC);
        
        OFGlobal.bindToShaderFrameBuffer.run();
        
    }
    
    @Override
    public void finishRendering() {
    
    }
    
    @Override
    protected void doRenderPortal(Portal portal, MatrixStack matrixStack) {
        if (RenderStates.getRenderedPortalNum() >= 1) {
            return;
        }
    
        PortalRendering.pushPortalLayer(portal);
        
        renderPortalContent(portal);
        //it will bind the gbuffer of rendered dimension
    
        PortalRendering.popPortalLayer();
        
        deferredBuffer.fb.beginWrite(true);
        
        GlStateManager.activeTexture(GL13.GL_TEXTURE0);
        client.getFramebuffer().draw(
            deferredBuffer.fb.viewportWidth,
            deferredBuffer.fb.viewportHeight
        );

        OFGlobal.bindToShaderFrameBuffer.run();
    }
    
    @Override
    public void invokeWorldRendering(
        RenderInfo renderInfo
    ) {
        MyGameRenderer.renderWorldNew(
            renderInfo,
            runnable -> {
                OFGlobal.shaderContextManager.switchContextAndRun(()->{
                    OFGlobal.bindToShaderFrameBuffer.run();
                    runnable.run();
                });
            }
        );
    }
    
    @Override
    public void onRenderCenterEnded(MatrixStack matrixStack) {
        if (PortalRendering.isRendering()) {
            return;
        }
        
        if (RenderStates.getRenderedPortalNum() == 0) {
            return;
        }
        
        GlStateManager.enableAlphaTest();
        Framebuffer mainFrameBuffer = client.getFramebuffer();
        mainFrameBuffer.beginWrite(true);
        
        deferredBuffer.fb.draw(mainFrameBuffer.viewportWidth, mainFrameBuffer.viewportHeight);
    }
}
