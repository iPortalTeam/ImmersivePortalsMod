package com.qouteall.immersive_portals.render;

import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.platform.GlStateManager;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.CHelper;
import com.qouteall.immersive_portals.exposer.IEGameRenderer;
import com.qouteall.immersive_portals.my_util.Helper;
import com.qouteall.immersive_portals.optifine_compatibility.OFHelper;
import com.qouteall.immersive_portals.portal.Portal;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GlFramebuffer;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.dimension.DimensionType;
import net.optifine.shaders.Shaders;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import java.util.Comparator;
import java.util.List;
import java.util.Stack;
import java.util.function.Supplier;

import static org.lwjgl.opengl.GL11.*;

public abstract class PortalRenderer {
    
    public static final MinecraftClient mc = MinecraftClient.getInstance();
    public Supplier<Integer> maxPortalLayer = () -> CGlobal.maxPortalLayer;
    public Supplier<Double> portalRenderingRange = () -> 64.0;
    protected Stack<Portal> portalLayers = new Stack<>();
    
    public static DimensionType originalPlayerDimension;
    protected Vec3d originalPlayerPos;
    protected Vec3d originalPlayerLastTickPos;
    protected GameMode originalGameMode;
    protected float partialTicks = 0;
    protected int idQueryObject = -1;
    protected Entity cameraEntity;
    protected int renderedPortalNum = 0;
    protected boolean isQuerying = false;
    
    protected void initIfNeeded() {
        if (idQueryObject == -1) {
            idQueryObject = GL15.glGenQueries();
            Helper.log("initialized renderer " + this.getClass());
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
    
    public abstract boolean shouldSkipClearing();
    
    public Portal getRenderingPortal() {
        return portalLayers.peek();
    }
    
    public Vec3d getOrignialPlayerPos() {
        return originalPlayerPos;
    }
    
    public Vec3d getOriginalPlayerLastTickPos() {
        return originalPlayerLastTickPos;
    }
    
    public GameMode getOriginalGameMode() {
        return originalGameMode;
    }
    
    public float getPartialTicks() {
        return partialTicks;
    }
    
    public boolean shouldRenderPlayerItself() {
        return isRendering() &&
            cameraEntity.dimension == originalPlayerDimension &&
            getRenderingPortal().canRenderEntityInsideMe(originalPlayerPos);
    }
    
    public boolean isDimensionRendered(DimensionType dimension) {
        assert dimension != null;
        if (dimension == CHelper.getOriginalDimension()) {
            return true;
        }
        return portalLayers
            .subList(0, portalLayers.size() - 1)//except for the current rendering portal
            .stream().anyMatch(
                portal -> portal.dimensionTo == dimension
            );
    }
    
    public void doRendering(float partialTicks_, long finishTimeNano_) {
        initIfNeeded();
        
        if (mc.cameraEntity == null) {
            return;
        }
        
        if (!isRendering()) {
            prepareRendering(partialTicks_, finishTimeNano_);
        }
        
        renderPortals();
        
        if (!isRendering()) {
            finishRendering();
        }
    }
    
    private void prepareRendering(float partialTicks_, long finishTimeNano_) {
        partialTicks = partialTicks_;
        cameraEntity = mc.cameraEntity;
        
        prepareStates();
        
        renderedPortalNum = 0;
    
        assert originalPlayerDimension == null;
        originalPlayerDimension = cameraEntity.dimension;
        originalPlayerPos = cameraEntity.getPos();
        originalPlayerLastTickPos = Helper.lastTickPosOf(cameraEntity);
        PlayerListEntry entry = CHelper.getClientPlayerListEntry();
        originalGameMode = entry != null ? entry.getGameMode() : GameMode.CREATIVE;
    }
    
    public boolean shouldRenderEntityNow(Entity entity) {
        if (isRendering()) {
            return getRenderingPortal().canRenderEntityInsideMe(entity.getPos());
        }
        return true;
    }
    
    protected abstract void prepareStates();
    
    private void finishRendering() {
        GL11.glClearStencil(0);
        GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
        GL11.glStencilFunc(GL_ALWAYS, 2333, 0xFF);
        GL11.glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP);
        
        GL11.glDisable(GL_STENCIL_TEST);
        GlStateManager.enableDepthTest();
        
        cameraEntity = null;
    
        CGlobal.clientWorldLoader
            .getDimensionRenderHelper(mc.world.dimension.getType())
            .switchToMe();
    
        originalPlayerDimension = null;
    }
    
    private void renderPortals() {
        assert cameraEntity.world == mc.world;
        assert cameraEntity.dimension == mc.world.dimension.getType();
        
        for (Portal portal : getPortalsNearbySorted()) {
            renderPortal(portal);
        }
    }
    
    private void renderPortal(Portal portal) {
        if (!portal.isPortalValid()) {
            Helper.err("rendering invalid portal " + portal);
            return;
        }
        
        //do not use last tick pos
        Vec3d thisTickEyePos = cameraEntity.getCameraPosVec(1);
        if (!portal.isInFrontOfPortal(thisTickEyePos)) {
            return;
        }
        
        if (isRendering()) {
            Portal outerPortal = portalLayers.peek();
            if (!outerPortal.canRenderPortalInsideMe(portal)) {
                return;
            }
        }
        
        doRenderPortal(portal);
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
    
    protected abstract void doRenderPortal(Portal portal);
    
    //it will overwrite the matrix
    //do not push matrix before calling this
    protected void manageCameraAndRenderPortalContent(
        Portal portal
    ) {
        if (getPortalLayer() > maxPortalLayer.get()) {
            return;
        }
    
        renderedPortalNum += 1;
        
        Entity cameraEntity = mc.cameraEntity;
        int allowedStencilValue = getPortalLayer();
        Camera camera = mc.gameRenderer.getCamera();
        
        assert cameraEntity.world == mc.world;
        
        Vec3d oldPos = cameraEntity.getPos();
        Vec3d oldLastTickPos = Helper.lastTickPosOf(cameraEntity);
        DimensionType oldDimension = cameraEntity.dimension;
        ClientWorld oldWorld = ((ClientWorld) cameraEntity.world);
        
        Vec3d oldCameraPos = camera.getPos();
        
        Vec3d newPos = portal.applyTransformationToPoint(oldPos);
        Vec3d newLastTickPos = portal.applyTransformationToPoint(oldLastTickPos);
        DimensionType newDimension = portal.dimensionTo;
        ClientWorld newWorld =
            CGlobal.clientWorldLoader.getOrCreateFakedWorld(newDimension);
        //Vec3d newCameraPos = portal.applyTransformationToPoint(oldCameraPos);
        
        Helper.setPosAndLastTickPos(cameraEntity, newPos, newLastTickPos);
        cameraEntity.dimension = newDimension;
        cameraEntity.world = newWorld;
        mc.world = newWorld;
    
        renderPortalContentWithContextSwitched(
            portal, oldCameraPos
        );
        
        //restore the position
        cameraEntity.dimension = oldDimension;
        cameraEntity.world = oldWorld;
        mc.world = oldWorld;
        Helper.setPosAndLastTickPos(cameraEntity, oldPos, oldLastTickPos);
        
        //restore the transformation
        GlStateManager.enableDepthTest();
        GlStateManager.disableBlend();
        setupCameraTransformation();
    }
    
    protected void renderPortalContentWithContextSwitched(
        Portal portal, Vec3d oldCameraPos
    ) {
        int thisPortalStencilValue = getPortalLayer();
        
        GlStateManager.enableAlphaTest();
        GlStateManager.enableCull();
        
        //draw content in the mask
        GL11.glStencilFunc(GL_EQUAL, thisPortalStencilValue, 0xFF);
        
        //do not manipulate stencil packetBuffer now
        GL11.glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP);
        
        WorldRenderer worldRenderer = CGlobal.clientWorldLoader.getWorldRenderer(portal.dimensionTo);
        ClientWorld destClientWorld = CGlobal.clientWorldLoader.getOrCreateFakedWorld(portal.dimensionTo);
        
        Helper.checkGlError();
        
        CGlobal.myGameRenderer.renderWorld(
            partialTicks, worldRenderer, destClientWorld, oldCameraPos
        );
        
        Helper.checkGlError();
        
    }
    
    public boolean renderAndGetDoesAnySamplePassed(Runnable renderingFunc) {
        assert (!isQuerying);
        
        GL15.glBeginQuery(GL15.GL_SAMPLES_PASSED, idQueryObject);
        
        isQuerying = true;
        
        renderingFunc.run();
        
        GL15.glEndQuery(GL15.GL_SAMPLES_PASSED);
        
        isQuerying = false;
        
        int result = GL15.glGetQueryObjecti(idQueryObject, GL15.GL_QUERY_RESULT);
        
        return result > 0;
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
        manageCameraAndRenderPortalContent(
            portal
        );
    }
    
    protected void setupCameraTransformation() {
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
    
    //it will render a box instead of a quad
    public void drawPortalViewTriangle(Portal portal) {
        DimensionRenderHelper helper =
            CGlobal.clientWorldLoader.getDimensionRenderHelper(portal.dimensionTo);
        
        Vec3d fogColor = helper.getFogColor();
        
        GlStateManager.disableCull();
        GlStateManager.disableAlphaTest();
        GlStateManager.disableTexture();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        GlStateManager.disableBlend();
        GlStateManager.disableLighting();
        
        GL11.glDisable(GL_CLIP_PLANE0);
    
        if (OFHelper.getIsUsingShader()) {
            fogColor = Vec3d.ZERO;
        }
        
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
    
    public abstract void renderPortalInEntityRenderer(Portal portal);
    
    public void onShaderRenderEnded() {
        //nothing
    }
    
    protected void drawFrameBufferUp(
        Portal portal,
        GlFramebuffer textureProvider,
        ShaderManager shaderManager
    ) {
        setupCameraTransformation();
        
        shaderManager.loadContentShaderAndShaderVars(0);
    
        if (OFHelper.getIsUsingShader()) {
            GlStateManager.viewport(
                0,
                0,
                mc.getFramebuffer().viewWidth,
                mc.getFramebuffer().viewHeight
            );
        }
        
        GlStateManager.enableTexture();
        
        GlStateManager.activeTexture(GLX.GL_TEXTURE0);
        
        GlStateManager.bindTexture(textureProvider.colorAttachment);
        GlStateManager.texParameter(3553, 10241, 9729);
        GlStateManager.texParameter(3553, 10240, 9729);
        GlStateManager.texParameter(3553, 10242, 10496);
        GlStateManager.texParameter(3553, 10243, 10496);
    
        drawPortalViewTriangle(portal);
        
        shaderManager.unloadShader();
    
        if (OFHelper.getIsUsingShader()) {
            GlStateManager.viewport(0, 0, Shaders.renderWidth, Shaders.renderHeight);
        }
    }
}
