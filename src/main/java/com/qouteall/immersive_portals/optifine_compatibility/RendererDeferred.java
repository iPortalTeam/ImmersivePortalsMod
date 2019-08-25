package com.qouteall.immersive_portals.optifine_compatibility;

import com.mojang.blaze3d.platform.GlStateManager;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.my_util.Helper;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.render.PortalRenderer;
import com.qouteall.immersive_portals.render.ShaderManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GlFramebuffer;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

public class RendererDeferred extends PortalRenderer {
    private GlFramebuffer deferredBuffer;
    private ShaderManager shaderManager;
    
    @Override
    public boolean shouldSkipClearing() {
        return false;
    }
    
    @Override
    protected void prepareStates() {
        GlFramebuffer mainFrameBuffer = mc.getFramebuffer();
        if (deferredBuffer == null) {
            deferredBuffer = new GlFramebuffer(
                mainFrameBuffer.viewWidth, mainFrameBuffer.viewHeight,
                true,//has depth attachment
                MinecraftClient.IS_SYSTEM_MAC
            );
        }
        if (mainFrameBuffer.viewWidth != deferredBuffer.viewWidth ||
            mainFrameBuffer.viewHeight != deferredBuffer.viewHeight
        ) {
            deferredBuffer.resize(
                mainFrameBuffer.viewWidth,
                mainFrameBuffer.viewHeight,
                MinecraftClient.IS_SYSTEM_MAC
            );
            Helper.log("Deferred buffer resized");
        }
        if (shaderManager == null) {
            shaderManager = new ShaderManager();
        }
    
        deferredBuffer.setClearColor(1, 0, 0, 0);
        deferredBuffer.clear(MinecraftClient.IS_SYSTEM_MAC);
    
        OFHelper.bindToShaderFrameBuffer();
    }
    
    @Override
    protected void doRenderPortal(Portal portal) {
        if (isRendering()) {
            //currently only support one-layer portal
            return;
        }
    
        copyDepthFromMainToDeferred();
    
        if (!testShouldRenderPortal(portal)) {
            return;
        }
    
        portalLayers.push(portal);
    
        manageCameraAndRenderPortalContent(portal);
        //it will bind the gbuffer of rendered dimension
    
        portalLayers.pop();
    
        deferredBuffer.beginWrite(true);
    
        drawFrameBufferUp(portal, mc.getFramebuffer(), shaderManager);
        
        OFHelper.bindToShaderFrameBuffer();
    }
    
    @Override
    protected void renderPortalContentWithContextSwitched(
        Portal portal, Vec3d oldCameraPos
    ) {
        OFGlobal.shaderContextManager.switchContextAndRun(
            () -> {
                OFHelper.bindToShaderFrameBuffer();
                super.renderPortalContentWithContextSwitched(portal, oldCameraPos);
            }
        );
    }
    
    @Override
    public void renderPortalInEntityRenderer(Portal portal) {
    
    }
    
    //NOTE it will write to depth buffer
    private boolean testShouldRenderPortal(Portal portal) {
        return renderAndGetDoesAnySamplePassed(() -> {
            GlStateManager.enableDepthTest();
            GlStateManager.disableTexture();
            setupCameraTransformation();
            GL20.glUseProgram(0);
            
            drawPortalViewTriangle(portal);
            
            GlStateManager.enableTexture();
        });
    }
    
    private void copyDepthFromMainToDeferred() {
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, OFGlobal.getDfb.get());
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, deferredBuffer.fbo);
        
        GL30.glBlitFramebuffer(
            0, 0, deferredBuffer.viewWidth, deferredBuffer.viewHeight,
            0, 0, deferredBuffer.viewWidth, deferredBuffer.viewHeight,
            GL11.GL_DEPTH_BUFFER_BIT, GL11.GL_NEAREST
        );
    
        OFHelper.bindToShaderFrameBuffer();
    }
    
    @Override
    public void onShaderRenderEnded() {
        if (isRendering()) {
            return;
        }
        
        if (renderedPortalNum == 0) {
            return;
        }
        
        GlStateManager.enableAlphaTest();
        mc.getFramebuffer().beginWrite(true);
    
        CGlobal.doDisableAlphaTestWhenRenderingFrameBuffer = false;
        deferredBuffer.draw(deferredBuffer.viewWidth, deferredBuffer.viewHeight);
        CGlobal.doDisableAlphaTestWhenRenderingFrameBuffer = true;
    }
}
