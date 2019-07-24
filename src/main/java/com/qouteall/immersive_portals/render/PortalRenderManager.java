package com.qouteall.immersive_portals.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.qouteall.immersive_portals.Globals;
import com.qouteall.immersive_portals.MyCommand;
import com.qouteall.immersive_portals.exposer.IEGameRenderer;
import com.qouteall.immersive_portals.my_util.Helper;
import com.qouteall.immersive_portals.portal.Portal;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.dimension.DimensionType;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBOcclusionQuery2;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import java.nio.FloatBuffer;
import java.util.Comparator;
import java.util.List;
import java.util.Stack;
import java.util.function.Supplier;

import static org.lwjgl.opengl.GL11.*;

//NOTE do not use glDisable(GL_DEPTH_TEST), use GlStateManager.disableDepthTest() instead
//because GlStateManager will cache its state. Do not make its cache not synchronized
public class PortalRenderManager {
    private MinecraftClient mc = MinecraftClient.getInstance();
    private Stack<Portal> portalLayers = new Stack<>();
    public Supplier<Integer> maxPortalLayer = () -> MyCommand.maxPortalLayer;
    public Supplier<Double> portalRenderingRange = () -> 64.0;
    private Runnable behavior;
    
    private float partialTicks = 0;
    private int idQueryObject = -1;
    private Entity cameraEntity;
    
    private int renderedPortalNum = 0;
    
    public PortalRenderManager() {
        behavior = this::renderPortals;
    }
    
    private void renderViewAreas() {
        GL11.glDisable(GL_STENCIL_TEST);
        
        setupCameraTransformation();
        
        getPortalsNearbySorted().forEach(this::drawPortalViewTriangle);
    }
    
    private void initIfNeeded() {
        if (idQueryObject == -1) {
            idQueryObject = GL15.glGenQueries();
        }
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
    
    public Portal getRenderingPortal() {
        return portalLayers.peek();
    }
    
    public boolean shouldSkipClearing() {
        return isRendering();
    }
    
    public void doRendering(float partialTicks_, long finishTimeNano_) {
        initIfNeeded();
        
        if (!isRendering()) {
            prepareRendering(partialTicks_, finishTimeNano_);
        }
        
        if (mc.cameraEntity == null) {
            return;
        }
        
        behavior.run();
        
        if (!isRendering()) {
            finishRendering();
        }
    }
    
    public boolean shouldRenderEntityNow(Entity entity) {
        if (isRendering()) {
            return getRenderingPortal().canRenderEntityInsideMe(entity.getPos());
        }
        return true;
    }
    
    private void prepareRendering(float partialTicks_, long finishTimeNano_) {
        partialTicks = partialTicks_;
        cameraEntity = mc.cameraEntity;
        
        //NOTE calling glClearStencil will not clear it, it just assigns the value for clearing
        GL11.glClearStencil(0);
        GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
        
        GlStateManager.enableDepthTest();
        GL11.glEnable(GL_STENCIL_TEST);
    
        renderedPortalNum = 0;
    }
    
    private void finishRendering() {
        GL11.glClearStencil(0);
        GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
        GL11.glStencilFunc(GL_ALWAYS, 2333, 0xFF);
        GL11.glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP);
        
        GL11.glDisable(GL_STENCIL_TEST);
        GlStateManager.enableDepthTest();
    
        cameraEntity = null;
    }
    
    private void renderPortals() {
        if (getPortalLayer() >= maxPortalLayer.get()) {
            return;
        }
        
        assert cameraEntity.world == mc.world;
        assert cameraEntity.dimension == mc.world.dimension.getType();
        
        for (Portal portal : getPortalsNearbySorted()) {
            renderPortal(portal);
        }
    }
    
    private List<Portal> getPortalsNearbySorted() {
        List<Portal> portalsNearby = mc.world.getEntities(
            Portal.class,
            new Box(cameraEntity.getBlockPos()).expand(portalRenderingRange.get())
        );
        
        portalsNearby.sort(
            Comparator.comparing(portalEntity ->
                portalEntity.getPos().squaredDistanceTo(cameraEntity.getPos())
            )
        );
        return portalsNearby;
    }
    
    private void renderPortal(Portal portal) {
        if (!portal.isPortalValid()) {
            Helper.err("rendering invalid portal " + portal);
            return;
        }
    
        //do not use last tick pos
        if (!portal.isInFrontOfPortal(cameraEntity.getPos())) {
            return;
        }
        
        int outerPortalStencilValue = getPortalLayer();
        
        if (isRendering()) {
            Portal outerPortal = portalLayers.peek();
            if (!outerPortal.canRenderPortalInsideMe(portal)) {
                return;
            }
        }
        
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
        
        managePlayerStateAndRenderPortalContent(portal);
        
        //the world rendering will modify the transformation
        setupCameraTransformation();
        
        restoreDepthOfPortalViewArea(portal);
        
        clampStencilValue(outerPortalStencilValue);
        
        //POP
        portalLayers.pop();
    }
    
    private void setupCameraTransformation() {
        ((IEGameRenderer) mc.gameRenderer).applyCameraTransformations_(partialTicks);
        Camera camera_1 = mc.gameRenderer.getCamera();
        camera_1.update(
            mc.world,
            (Entity) (mc.getCameraEntity() == null ? mc.player : mc.getCameraEntity()),
            mc.options.perspective > 0,
            mc.options.perspective == 2,
            partialTicks
        );
        
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
        
        drawPortalViewTriangle(portal);
        
        GlStateManager.enableBlend();
        
        Helper.checkGlError();
    }
    
    //it will render a box instead of a quad
    private void drawPortalViewTriangle(Portal portal) {
        Globals.shaderManager.unloadShader();
        
        DimensionRenderHelper helper =
            Globals.clientWorldLoader.getDimensionRenderHelper(portal.dimensionTo);
    
        Vec3d fogColor = helper.getFogColor();
        
        GlStateManager.disableCull();
        GlStateManager.disableAlphaTest();
        GlStateManager.disableTexture();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        GlStateManager.disableBlend();
        GlStateManager.disableLighting();
        
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBufferBuilder();
        ViewAreaRenderer.buildPortalViewAreaTrianglesBuffer(
            fogColor,
            portal,
            bufferbuilder,
            mc.gameRenderer.getCamera().getPos(),
            partialTicks
        );
        
        tessellator.draw();
        
        GlStateManager.enableCull();
        GlStateManager.enableAlphaTest();
        GlStateManager.enableTexture();
        GlStateManager.enableLighting();
    }
    
    //it will overwrite the matrix
    //do not push matrix before calling this
    private void managePlayerStateAndRenderPortalContent(
        Portal portal
    ) {
        Entity player = mc.cameraEntity;
        int allowedStencilValue = getPortalLayer();
        
        assert player.world == mc.world;
        
        Vec3d originalPos = player.getPos();
        Vec3d originalLastTickPos = Helper.lastTickPosOf(player);
        DimensionType originalDimension = player.dimension;
        ClientWorld originalWorld = ((ClientWorld) player.world);
        
        Vec3d newPos = portal.applyTransformationToPoint(originalPos);
        Vec3d newLastTickPos = portal.applyTransformationToPoint(originalLastTickPos);
        DimensionType newDimension = portal.dimensionTo;
        ClientWorld newWorld =
            Globals.clientWorldLoader.getOrCreateFakedWorld(newDimension);
        
        Helper.setPosAndLastTickPos(player, newPos, newLastTickPos);
        player.dimension = newDimension;
        player.world = newWorld;
        mc.world = newWorld;
        
        renderPortalContentAndNestedPortals(
            portal
        );
        
        //restore the position
        player.dimension = originalDimension;
        player.world = originalWorld;
        mc.world = originalWorld;
        Helper.setPosAndLastTickPos(player, originalPos, originalLastTickPos);
        
        //restore the transformation
        GlStateManager.enableDepthTest();
        GlStateManager.disableBlend();
        //setupCameraTransformation();
    }
    
    private void renderPortalContentAndNestedPortals(
        Portal portal
    ) {
        int thisPortalStencilValue = getPortalLayer();
        
        GlStateManager.enableAlphaTest();
        GlStateManager.enableCull();
        
        //draw content in the mask
        GL11.glStencilFunc(GL_EQUAL, thisPortalStencilValue, 0xFF);
        
        //do not manipulate stencil packetBuffer now
        GL11.glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP);
        
        WorldRenderer worldRenderer = Globals.clientWorldLoader.getWorldRenderer(portal.dimensionTo);
        ClientWorld destClientWorld = Globals.clientWorldLoader.getOrCreateFakedWorld(portal.dimensionTo);
        
        Globals.myGameRenderer.renderWorld(
            partialTicks, worldRenderer, destClientWorld
        );
        
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
        
        Globals.shaderManager.unloadShader();
        
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
    
    private void debugRenderPortalContent(int portalId) {
        Portal portal = ((Portal) mc.world.getEntityById(portalId));
        if (portal == null) {
            Helper.err("debugging nonexistent portal?");
            return;
        }
        
        GL11.glClearColor(0, 0, 0, 1);
        GL11.glClearDepth(1);
        GL11.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        
        //NOTE in state_portalViewOfTheFirstPortal, max PortalEntity layer is 1
        //so that it will not render nested portals
        managePlayerStateAndRenderPortalContent(
            portal
        );
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
