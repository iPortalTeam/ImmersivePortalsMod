package com.qouteall.immersive_portals.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.CHelper;
import com.qouteall.immersive_portals.ducks.IEFrameBuffer;
import com.qouteall.immersive_portals.portal.Portal;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.GL_ALWAYS;
import static org.lwjgl.opengl.GL11.GL_DEPTH_FUNC;
import static org.lwjgl.opengl.GL11.GL_DEPTH_RANGE;
import static org.lwjgl.opengl.GL11.GL_EQUAL;
import static org.lwjgl.opengl.GL11.GL_INCR;
import static org.lwjgl.opengl.GL11.GL_KEEP;
import static org.lwjgl.opengl.GL11.GL_LEQUAL;
import static org.lwjgl.opengl.GL11.GL_LESS;
import static org.lwjgl.opengl.GL11.GL_REPLACE;
import static org.lwjgl.opengl.GL11.GL_STENCIL_TEST;

//NOTE do not use glDisable(GL_DEPTH_TEST), use GlStateManager.disableDepthTest() instead
//because GlStateManager will cache its state. Do not make its cache not synchronized
public class RendererUsingStencil extends PortalRenderer {
    
    @Override
    public boolean shouldSkipClearing() {
        return isRendering();
    }
    
    @Override
    public void onBeforeTranslucentRendering(MatrixStack matrixStack) {
        if (!isRendering()) {
            doPortalRendering(matrixStack);
        }
    }
    
    private void doPortalRendering(MatrixStack matrixStack) {
        mc.getProfiler().swap("render_portal_total");
        renderPortals(matrixStack);
        if (!isRendering()) {
            myFinishRendering();
        }
    }
    
    @Override
    public void onAfterTranslucentRendering(MatrixStack matrixStack) {
        if (isRendering()) {
            doPortalRendering(matrixStack);
        }
    }
    
    @Override
    public void onRenderCenterEnded(MatrixStack matrixStack) {
        //nothing
    }
    
    @Override
    public void prepareRendering() {
        //NOTE calling glClearStencil will not clear it, it just assigns the value for clearing
        GL11.glClearStencil(0);
        GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
    
        GlStateManager.enableDepthTest();
        GL11.glEnable(GL_STENCIL_TEST);
    
        ((IEFrameBuffer) mc.getFramebuffer())
            .setIsStencilBufferEnabledAndReload(true);
    }
    
    @Override
    public void finishRendering() {
        //nothing
    }
    
    private void myFinishRendering() {
        GL11.glStencilFunc(GL_ALWAYS, 2333, 0xFF);
        GL11.glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP);
        
        GL11.glDisable(GL_STENCIL_TEST);
        GlStateManager.enableDepthTest();
    }
    
    @Override
    protected void doRenderPortal(
        Portal portal,
        MatrixStack matrixStack
    ) {
        int outerPortalStencilValue = getPortalLayer();
        
        mc.getProfiler().push("render_view_area");
        boolean anySamplePassed = QueryManager.renderAndGetDoesAnySamplePassed(() -> {
            renderPortalViewAreaToStencil(portal, matrixStack);
        });
        mc.getProfiler().pop();
        
        if (!anySamplePassed) {
            setStencilStateForWorldRendering();
            return;
        }
    
        //PUSH
        portalLayers.push(portal);
        
        int thisPortalStencilValue = outerPortalStencilValue + 1;
    
        mc.getProfiler().push("clear_depth_of_view_area");
        clearDepthOfThePortalViewArea(portal);
        mc.getProfiler().pop();
    
        manageCameraAndRenderPortalContent(portal);
    
        restoreDepthOfPortalViewArea(portal, matrixStack);
    
        clampStencilValue(outerPortalStencilValue);
    
        //is it necessary?
        CGlobal.myGameRenderer.resetDiffuseLighting(matrixStack);
    
        //POP
        portalLayers.pop();
    }
    
    @Override
    public void renderPortalInEntityRenderer(Portal portal) {
        //nothing
    }
    
    @Override
    protected void renderPortalContentWithContextSwitched(
        Portal portal, Vec3d oldCameraPos, ClientWorld oldWorld
    ) {
        setStencilStateForWorldRendering();
    
        super.renderPortalContentWithContextSwitched(portal, oldCameraPos, oldWorld);
    }
    
    private void renderPortalViewAreaToStencil(
        Portal portal, MatrixStack matrixStack
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
        
        GlStateManager.disableBlend();
    
        GL20.glUseProgram(0);
    
        RenderSystem.enableDepthTest();
        GlStateManager.depthMask(true);
    
        ViewAreaRenderer.drawPortalViewTriangle(portal, matrixStack, true);
        
        GlStateManager.enableBlend();
    
        CHelper.checkGlError();
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
    
        MyRenderHelper.renderScreenTriangle();
        
        //retrieve the state
        GL11.glColorMask(true, true, true, true);
        GL11.glDepthFunc(originalDepthFunc);
        GL11.glDepthRange(originalDepthRange.get(0), originalDepthRange.get(1));
    }
    
    private void restoreDepthOfPortalViewArea(
        Portal portal, MatrixStack matrixStack
    ) {
        int thisPortalStencilValue = getPortalLayer();
        
        //do not manipulate stencil packetBuffer
        GL11.glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP);
    
        //only draw in portalView area
        GL11.glStencilFunc(GL_LEQUAL, thisPortalStencilValue, 0xFF);
    
        //do not manipulate color packetBuffer
        GL11.glColorMask(false, false, false, false);
    
        //do manipulate the depth packetBuffer
        GL11.glDepthMask(true);
    
        GL20.glUseProgram(0);
    
        GlStateManager.enableDepthTest();
    
        ViewAreaRenderer.drawPortalViewTriangle(portal, matrixStack, false);
    
        GL11.glColorMask(true, true, true, true);
    }
    
    public static void clampStencilValue(
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
    
        MyRenderHelper.renderScreenTriangle();
    
        GL11.glDepthMask(true);
    
        GL11.glColorMask(true, true, true, true);
    
        GlStateManager.enableDepthTest();
    }
    
    private void setStencilStateForWorldRendering() {
        int thisPortalStencilValue = getPortalLayer();
        
        //draw content in the mask
        GL11.glStencilFunc(GL_EQUAL, thisPortalStencilValue, 0xFF);
        
        //do not manipulate stencil packetBuffer now
        GL11.glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP);
    }
}
