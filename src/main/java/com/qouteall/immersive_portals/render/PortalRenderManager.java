package com.qouteall.immersive_portals.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.qouteall.immersive_portals.Globals;
import com.qouteall.immersive_portals.exposer.IEGameRenderer;
import com.qouteall.immersive_portals.my_util.Helper;
import com.qouteall.immersive_portals.portal_entity.PortalEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.dimension.DimensionType;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBOcclusionQuery2;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import java.nio.FloatBuffer;
import java.util.*;
import java.util.function.Supplier;

import static org.lwjgl.opengl.GL11.*;

public class PortalRenderManager {
    private MinecraftClient mc = MinecraftClient.getInstance();
    private Stack<PortalEntity> portalLayers = new Stack<>();
    public Supplier<Integer> maxPortalLayer = () -> 2;
    public Supplier<Double> portalRenderingRange = () -> 64.0;
    private Runnable behavior;
    
    private float partialTicks = 0;
    private long finishTimeNano = 0;
    private Entity cameraEntity;
    
    public PortalRenderManager() {
        behavior = this::renderPortals;
    }
    
    //0 for rendering outer world
    //1 for rendering world inside portal
    //2 for rendering world inside PortalEntity inside portal
    public int getPortalLayer() {
        return portalLayers.size();
    }
    
    public boolean isRendering() {
        return getPortalLayer() != 0;
    }
    
    public PortalEntity getRenderingPortalData() {
        return portalLayers.peek();
    }
    
    public void doRendering(float partialTicks_, long finishTimeNano_) {
        if (cameraEntity == null) {
            return;
        }
        
        if (!isRendering()) {
            prepareRendering(partialTicks, finishTimeNano);
        }
        
        behavior.run();
        
        if (!isRendering()) {
            finishRendering();
        }
    }
    
    private void prepareRendering(float partialTicks_, long finishTimeNano_) {
        partialTicks = partialTicks_;
        finishTimeNano = finishTimeNano_;
        cameraEntity = mc.cameraEntity;
        
        //NOTE calling glClearStencil will not clear it, it just assigns the value for clearing
        GL11.glClearStencil(0);
        GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
        
        GL11.glEnable(GL_DEPTH_TEST);
        GL11.glEnable(GL_STENCIL_TEST);
    }
    
    private void finishRendering() {
        GL11.glClearStencil(0);
        GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
        GL11.glStencilFunc(GL_ALWAYS, 2333, 0xFF);
        GL11.glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP);
        
        GL11.glDisable(GL_STENCIL_TEST);
        
        GL11.glColorMask(true, true, true, true);
    }
    
    
    private void renderPortals() {
        if (getPortalLayer() >= maxPortalLayer.get()) {
            return;
        }
        
        assert cameraEntity.world == mc.world;
        assert cameraEntity.dimension == mc.world.dimension.getType();
        
        List<PortalEntity> portalsNearby = mc.world.getEntities(
            PortalEntity.class,
            new Box(cameraEntity.getBlockPos()).expand(portalRenderingRange.get())
        );
        
        portalsNearby.sort(
            Comparator.comparing(portalEntity ->
                portalEntity.getPos().squaredDistanceTo(cameraEntity.getPos())
            )
        );
        
        for (PortalEntity portal : portalsNearby) {
            renderPortal(portal);
        }
    }
    
    private void renderPortal(PortalEntity portal) {
        if (!portal.canSeeThroughFromPos(cameraEntity.getPos())) {
            return;
        }
        
        setupCameraTransformation();
        
        int outerPortalStencilValue = getPortalLayer();
        PortalEntity outerPortalEntity = portalLayers.peek();
        
        boolean anySamplePassed = renderAndGetDoesAnySamplePassed(() -> {
            renderPortalViewAreaToStencil(
                outerPortalStencilValue,
                portal,
                partialTicks,
                outerPortal
            );
        });
        
        if (!anySamplePassed) {
            return;
        }
        
        portalLayers.push(portal);
        
        int thisPortalStencilValue = outerPortalStencilValue + 1;
        
        clearDepthOfThePortalViewArea(thisPortalStencilValue, portal, partialTicks);
        
        //it will setup camera transformation again and overwrite the current
        managePlayerStateAndRenderPortalContent(
            thisPortalStencilValue, portal, partialTicks
        );
        
        //the world rendering may modify the transformation
        setupCameraTransformation();
        
        restoreDepthOfPortalViewArea(
            thisPortalStencilValue,
            portal,
            partialTicks
        );
        
        clampStencilValue(
            outerPortalStencilValue
        );
        
        portalLayers.pop();
    }
    
    private void setupCameraTransformation() {
        ((IEGameRenderer) mc.gameRenderer).applyCameraTransformations_(partialTicks);
    }
    
    private void renderPortalViewAreaToStencil(
        int outerPortalStencilValue,
        PortalEntity portal,
        float partialTicks,
        PortalEntity outerPortal
    ) {
        //is the mask here different from the mask of glStencilMask?
        GL11.glStencilFunc(GL_EQUAL, outerPortalStencilValue, 0xFF);
        
        //if stencil and depth test pass, the data in stencil packetBuffer will increase by 1
        GL11.glStencilOp(GL_KEEP, GL_KEEP, GL_INCR);
        //NOTE about GL_INCR:
        //if multiple triangles occupy the same pixel and passed stencil and depth tests,
        //its stencil value will still increase by one
        
        GL11.glStencilMask(0xFF);
        GlStateManager.depthMask(true);
        
        drawPortalViewTriangle(partialTicks, portal);
        
        Helper.checkGlError();
    }
    
    //it will render a box instead of a quad
    private void drawPortalViewTriangle(float partialTicks, PortalEntity portal) {
        Globals.shaderManager.unloadShader();
        
        DimensionRenderHelper helper = Globals.gameRenderer.getRenderHelper(portal.dimensionTo);
        Vec3d fogColor = helper.getFogColor();
        
        GlStateManager.disableCull();
        GlStateManager.disableAlphaTest();
        GlStateManager.disableTexture();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBufferBuilder();
        ViewAreaRenderer.buildPortalViewAreaTrianglesBuffer(
            fogColor,
            portal,
            bufferbuilder,
            cameraEntity
        );
        
        tessellator.draw();
        
        GlStateManager.enableCull();
        GlStateManager.enableAlphaTest();
        GlStateManager.enableTexture2D();
    }
    
    //it will overwrite the matrix
    //do not push matrix before calling this
    private void managePlayerStateAndRenderPortalContent(
        int allowedStencilValue, PortalEntity portal, float partialTicks
    ) {
        Entity player = mc.getRenderViewEntity();
        
        assert player.world == mc.world;
        
        Vec3d originalPos = player.getPositionVector();
        Vec3d originalLastTickPos = Helper.lastTickPosOf(player);
        DimensionType originalDimension = player.dimension;
        WorldClient originalWorld = ((WorldClient) player.world);
        
        Vec3d newPos = portal.applyTransformationToPoint(originalPos);
        Vec3d newLastTickPos = portal.applyTransformationToPoint(originalLastTickPos);
        DimensionType newDimension = portal.dimensionTo;
        WorldClient newWorld =
            Globals.portalManagerClient.worldLoader.getOrCreateFakedWorld(newDimension);
        
        Helper.setPosAndLastTickPos(player, newPos, newLastTickPos);
        player.dimension = newDimension;
        player.world = newWorld;
        mc.world = newWorld;
        
        MyViewFrustum.updateViewFrustum();
        
        renderPortalContentAndNestedPortals(
            allowedStencilValue, portal, partialTicks
        );
        
        //restore the position
        player.dimension = originalDimension;
        player.world = originalWorld;
        mc.world = originalWorld;
        Helper.setPosAndLastTickPos(player, originalPos, originalLastTickPos);
    }
    
    private void renderPortalContentAndNestedPortals(
        int thisPortalStencilValue, PortalEntity portal, float partialTicks
    ) {
        GlStateManager.enableAlphaTest();
        GlStateManager.enableCull();
        
        //draw content in the mask
        GL11.glStencilFunc(GL_EQUAL, thisPortalStencilValue, 0xFF);
        
        //do not manipulate stencil packetBuffer now
        GL11.glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP);
        
        renderingPortalEntity = portal;//this will be used in loadShaderAndMatrix()
        
        _debugBuffers();
        
        //it will overwrite the matrix
        Globals.gameRenderer.renderDimensionWorld(
            portal.dimensionTo, partialTicks
        );
        
        _debugBuffers();
        
        Helper.checkGlError();
        
        renderingPortalEntity = null;
        
        //render nested portals
        renderPortals(thisPortalStencilValue, partialTicks, portal);
    }
    
    private void renderScreenTriangle() {
        GlStateManager.matrixMode(GL_MODELVIEW);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        
        GlStateManager.matrixMode(GL_PROJECTION);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        
        GlStateManager.disableAlphaTest();
        GlStateManager.disableTexture2D();
        
        GlStateManager.shadeModel(GL_SMOOTH);
        
        Globals.gameRenderer.unloadShader();
        
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
        
        bufferbuilder.pos(1, -1, 0).color(255, 255, 255, 255)
            .endVertex();
        bufferbuilder.pos(1, 1, 0).color(255, 255, 255, 255)
            .endVertex();
        bufferbuilder.pos(-1, 1, 0).color(255, 255, 255, 255)
            .endVertex();
        
        bufferbuilder.pos(-1, 1, 0).color(255, 255, 255, 255)
            .endVertex();
        bufferbuilder.pos(-1, -1, 0).color(255, 255, 255, 255)
            .endVertex();
        bufferbuilder.pos(1, -1, 0).color(255, 255, 255, 255)
            .endVertex();
        
        tessellator.draw();
        
        GlStateManager.matrixMode(GL_MODELVIEW);
        GlStateManager.popMatrix();
        
        GlStateManager.matrixMode(GL_PROJECTION);
        GlStateManager.popMatrix();
        
        GlStateManager.enableAlphaTest();
        GlStateManager.enableTexture2D();
    }
    
    private void clearDepthOfThePortalViewArea(
        int allowedStencilValue,
        PortalEntity portal,
        float partialTicks
    ) {
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
        int thisPortalStencilValue,
        PortalEntity portal,
        float partialTicks
    ) {
        //do not manipulate stencil packetBuffer
        GL11.glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP);
        
        //only draw in its PortalEntity view area and nested portal's view area
        GL11.glStencilFunc(GL_EQUAL, thisPortalStencilValue, 0xFF);
        
        //do not manipulate color packetBuffer
        GL11.glColorMask(false, false, false, false);
        
        //do manipulate the depth packetBuffer
        GL11.glDepthMask(true);
        
        drawPortalViewTriangle(partialTicks, portal);
        
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
        
        GL11.glDisable(GL_DEPTH_TEST);
        
        renderScreenTriangle();
        
        GL11.glDepthMask(true);
        
        GL11.glColorMask(true, true, true, true);
        
        GL11.glEnable(GL_DEPTH_TEST);
    }
    
    private boolean isQuerying = false;
    
    private boolean renderAndGetDoesAnySamplePassed(Runnable renderingFunc) {
        assert (!isQuerying);
        
        GL15.glBeginQuery(ARBOcclusionQuery2.GL_ANY_SAMPLES_PASSED, idQueryObject);
        
        isQuerying = true;
        
        renderingFunc.run();
        
        GL15.glEndQuery(ARBOcclusionQuery2.GL_ANY_SAMPLES_PASSED);
        
        isQuerying = false;
        
        int result = GL15.glGetQueryObjecti(idQueryObject, GL15.GL_QUERY_RESULT);
        
        return result != 0;
    }
    
    private int renderAndGetSampleCountPassed(Runnable renderingFunc) {
        assert (!isQuerying);
        
        GL15.glBeginQuery(GL15.GL_SAMPLES_PASSED, idQueryObject);
        
        isQuerying = true;
        
        renderingFunc.run();
        
        GL15.glEndQuery(GL15.GL_SAMPLES_PASSED);
        
        isQuerying = false;
        
        int result = GL15.glGetQueryObjecti(idQueryObject, GL15.GL_QUERY_RESULT);
        
        return result;
    }
    
    private void debugRenderPortalContent(int portalId, float partialTicks) {
        PortalEntity PortalEntity = PortalDataManager.getDataManagerOnClient().getPortal(portalId);
        if (PortalEntity == null) {
            Helper.err("debugging nonexistent portal?");
            return;
        }
        
        GL11.glClearColor(0, 0, 0, 1);
        GL11.glClearDepth(1);
        GL11.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        
        //NOTE in state_portalViewOfTheFirstPortal, max PortalEntity layer is 1
        //so that it will not render nested portals
        managePlayerStateAndRenderPortalContent(
            0, portal, partialTicks
        );
    }
    
    public void renderViewArea(PortalEntity portal, float partialTicks) {
        Entity renderViewEntity = mc.getRenderViewEntity();
        
        if (!portal.canSeeThroughFromPos(renderViewEntity.getPositionVector())) {
            return;
        }
        
        Globals.gameRenderer.getRenderHelper(portal.dimensionTo)
            .fogRenderer.updateFogColor(0);
        
        setupPlayerCameraTransform(partialTicks);
        
        renderPortalViewAreaToStencil(0, portal, partialTicks, null);
    }
    
}
