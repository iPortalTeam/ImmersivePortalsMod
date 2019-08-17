package com.qouteall.immersive_portals.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.qouteall.immersive_portals.exposer.IEGlFrameBuffer;
import com.qouteall.immersive_portals.my_util.Helper;
import com.qouteall.immersive_portals.portal.Portal;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.entity.Entity;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;

//NOTE do not use glDisable(GL_DEPTH_TEST), use GlStateManager.disableDepthTest() instead
//because GlStateManager will cache its state. Do not make its cache not synchronized
public class RendererUsingStencil extends PortalRenderer {
    
    @Override
    public boolean shouldSkipClearing() {
        return isRendering();
    }
    
    @Override
    protected void initIfNeeded() {
        super.initIfNeeded();
        IEGlFrameBuffer framebuffer = (IEGlFrameBuffer) MinecraftClient.getInstance().getFramebuffer();
        framebuffer.setIsStencilBufferEnabledAndReload(true);
    }
    
    @Override
    protected void prepareStates() {
        //NOTE calling glClearStencil will not clear it, it just assigns the value for clearing
        GL11.glClearStencil(0);
        GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
        
        GlStateManager.enableDepthTest();
        GL11.glEnable(GL_STENCIL_TEST);
    }
    
    @Override
    protected void doRenderPortal(Portal portal) {
        int outerPortalStencilValue = getPortalLayer();
        
        setupCameraTransformation();
        
        boolean anySamplePassed = renderAndGetDoesAnySamplePassed(() -> {
            renderPortalViewAreaToStencil(portal);
        });
        
        if (!anySamplePassed) {
            return;
        }
        
        renderedPortalNum += 1;
        
        //PUSH
        portalLayers.push(portal);
        
        int thisPortalStencilValue = outerPortalStencilValue + 1;
        
        clearDepthOfThePortalViewArea(portal);
        
        manageCameraAndRenderPortalContent(portal);
        
        //the world rendering will modify the transformation
        setupCameraTransformation();
        
        restoreDepthOfPortalViewArea(portal);
        
        clampStencilValue(outerPortalStencilValue);
        
        //POP
        portalLayers.pop();
    }
    
    @Override
    public void renderPortalInEntityRenderer(Portal portal) {
        //nothing
    }
    
    private void renderPortalViewAreaToStencil(
        Portal portal
    ) {
        int outerPortalStencilValue = getPortalLayer();
        
        //is the mask here different from the mask of glStencilMask?
        GL11.glStencilFunc(GL_EQUAL, outerPortalStencilValue, 0xFF);
        
        //if stencil and depth test pass, the data in stencil packetBuffer will increase by 1
        GL11.glStencilOp(GL_KEEP, GL_KEEP, GL_INCR);
        //NOTE about GL_INCR:
        //if multiple triangles occupy the same pixel and passed stencil and depth tests,
        //its stencil value will still increase by one
        
        GL11.glStencilMask(0xFF);
        GlStateManager.depthMask(true);
        
        GlStateManager.disableBlend();
    
        GL20.glUseProgram(0);
        
        drawPortalViewTriangle(portal);
        
        GlStateManager.enableBlend();
        
        Helper.checkGlError();
    }
    
    private void renderScreenTriangle() {
        GlStateManager.matrixMode(GL_MODELVIEW);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        
        GlStateManager.matrixMode(GL_PROJECTION);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        
        GlStateManager.disableAlphaTest();
        GlStateManager.disableTexture();
        
        GlStateManager.shadeModel(GL_SMOOTH);
    
        GL20.glUseProgram(0);
        GL11.glDisable(GL_CLIP_PLANE0);
        
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBufferBuilder();
        bufferbuilder.begin(GL_TRIANGLES, VertexFormats.POSITION_COLOR);
        
        bufferbuilder.vertex(1, -1, 0).color(255, 255, 255, 255)
            .next();
        bufferbuilder.vertex(1, 1, 0).color(255, 255, 255, 255)
            .next();
        bufferbuilder.vertex(-1, 1, 0).color(255, 255, 255, 255)
            .next();
        
        bufferbuilder.vertex(-1, 1, 0).color(255, 255, 255, 255)
            .next();
        bufferbuilder.vertex(-1, -1, 0).color(255, 255, 255, 255)
            .next();
        bufferbuilder.vertex(1, -1, 0).color(255, 255, 255, 255)
            .next();
        
        tessellator.draw();
        
        GlStateManager.matrixMode(GL_MODELVIEW);
        GlStateManager.popMatrix();
        
        GlStateManager.matrixMode(GL_PROJECTION);
        GlStateManager.popMatrix();
        
        GlStateManager.enableAlphaTest();
        GlStateManager.enableTexture();
    }
    
    private void clearDepthOfThePortalViewArea(
        Portal portal
    ) {
        int allowedStencilValue = getPortalLayer();
        
        GlStateManager.enableDepthTest();
        
        //do not manipulate stencil packetBuffer
        GL11.glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP);
        
        //only draw in portalView area
        GL11.glStencilFunc(GL_EQUAL, allowedStencilValue, 0xFF);
        
        //do not manipulate color packetBuffer
        GL11.glColorMask(false, false, false, false);
        
        //save the state
        int originalDepthFunc = GL11.glGetInteger(GL_DEPTH_FUNC);
        FloatBuffer originalDepthRange = BufferUtils.createFloatBuffer(16);
        GL11.glGetFloatv(GL_DEPTH_RANGE, originalDepthRange);
        
        //always passes depth test
        GL11.glDepthFunc(GL_ALWAYS);
        
        //the pixel's depth will be 1, which is the furthest
        GL11.glDepthRange(1, 1);
        
        renderScreenTriangle();
        //drawPortalViewTriangle(partialTicks, portal);
        
        //retrieve the state
        GL11.glColorMask(true, true, true, true);
        GL11.glDepthFunc(originalDepthFunc);
        GL11.glDepthRange(originalDepthRange.get(0), originalDepthRange.get(1));
    }
    
    private void restoreDepthOfPortalViewArea(
        Portal portal
    ) {
        int thisPortalStencilValue = getPortalLayer();
        
        //do not manipulate stencil packetBuffer
        GL11.glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP);
        
        //only draw in its PortalEntity view area and nested portal's view area
        GL11.glStencilFunc(GL_EQUAL, thisPortalStencilValue, 0xFF);
        
        //do not manipulate color packetBuffer
        GL11.glColorMask(false, false, false, false);
        
        //do manipulate the depth packetBuffer
        GL11.glDepthMask(true);
    
        GL20.glUseProgram(0);
        
        drawPortalViewTriangle(portal);
        
        GL11.glColorMask(true, true, true, true);
    }
    
    private void clampStencilValue(
        int maximumValue
    ) {
        //NOTE GL_GREATER means ref > stencil
        //GL_LESS means ref < stencil
        //"greater" does not mean "greater than ref"
        //It's very unintuitive
        
        //pass if the stencil value is greater than the maximum value
        GL11.glStencilFunc(GL_LESS, maximumValue, 0xFF);
        
        //if stencil test passed, encode the stencil value
        GL11.glStencilOp(GL_KEEP, GL_REPLACE, GL_REPLACE);
        
        //do not manipulate the depth packetBuffer
        GL11.glDepthMask(false);
        
        //do not manipulate the color packetBuffer
        GL11.glColorMask(false, false, false, false);
        
        GlStateManager.disableDepthTest();
        
        renderScreenTriangle();
        
        GL11.glDepthMask(true);
        
        GL11.glColorMask(true, true, true, true);
        
        GlStateManager.enableDepthTest();
    }
    
    public void renderViewArea(Portal portal) {
        Entity renderViewEntity = mc.cameraEntity;
        
        if (!portal.isInFrontOfPortal(renderViewEntity.getPos())) {
            return;
        }
        
        //TODO maybe should update fog color here?
        
        setupCameraTransformation();
        
        renderPortalViewAreaToStencil(portal);
    }
    
}
