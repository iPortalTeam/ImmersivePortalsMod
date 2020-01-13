package com.qouteall.immersive_portals.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.qouteall.immersive_portals.*;
import com.qouteall.immersive_portals.portal.Portal;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.dimension.DimensionType;

import java.util.Comparator;
import java.util.List;
import java.util.Stack;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public abstract class PortalRenderer {
    
    public static final MinecraftClient mc = MinecraftClient.getInstance();
    protected Supplier<Integer> maxPortalLayer = () -> CGlobal.maxPortalLayer;
    protected Stack<Portal> portalLayers = new Stack<>();
    
    //this WILL be called when rendering portal
    public abstract void onBeforeTranslucentRendering(MatrixStack matrixStack);
    
    //this WILL be called when rendering portal
    public abstract void onAfterTranslucentRendering(MatrixStack matrixStack);
    
    //this WILL be called when rendering portal
    public abstract void onRenderCenterEnded(MatrixStack matrixStack);
    
    //this will NOT be called when rendering portal
    public abstract void prepareRendering();
    
    //this will NOT be called when rendering portal
    public abstract void finishRendering();
    
    //this will be called when rendering portal entities
    public abstract void renderPortalInEntityRenderer(Portal portal);
    
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
    
    public boolean shouldRenderPlayerItself() {
        return isRendering() &&
            mc.cameraEntity.dimension == MyRenderHelper.originalPlayerDimension &&
            getRenderingPortal().canRenderEntityInsideMe(MyRenderHelper.originalPlayerPos);
    }
    
    public boolean shouldRenderEntityNow(Entity entity) {
        if (OFInterface.isShadowPass.getAsBoolean()) {
            return true;
        }
        if (isRendering()) {
            return getRenderingPortal().canRenderEntityInsideMe(entity.getPos());
        }
        return true;
    }
    
    protected void renderPortals(MatrixStack matrixStack) {
        assert mc.cameraEntity.world == mc.world;
        assert mc.cameraEntity.dimension == mc.world.dimension.getType();
    
        //currently does not support nested portal rendering in mirror
        if (MyRenderHelper.isRenderingMirror()) {
            return;
        }
        
        for (Portal portal : getPortalsNearbySorted()) {
            renderPortalIfRoughCheckPassed(portal, matrixStack);
        }
    }
    
    private void renderPortalIfRoughCheckPassed(
        Portal portal,
        MatrixStack matrixStack
    ) {
        if (!portal.isPortalValid()) {
            Helper.err("rendering invalid portal " + portal);
            return;
        }
    
        Vec3d thisTickEyePos = getRoughTestCameraPos();
    
        if (!portal.isInFrontOfPortal(thisTickEyePos)) {
            return;
        }
        
        if (isRendering()) {
            //avoid rendering reverse portal inside portal
            Portal outerPortal = portalLayers.peek();
            if (!outerPortal.canRenderPortalInsideMe(portal)) {
                return;
            }
        }
        
        doRenderPortal(portal, matrixStack);
    }
    
    private Vec3d getRoughTestCameraPos() {
        if (mc.gameRenderer.getCamera().isThirdPerson()) {
            return mc.gameRenderer.getCamera().getPos();
        }
        if (CGlobal.teleportOnRendering) {
            return mc.cameraEntity.getCameraPosVec(MyRenderHelper.partialTicks);
        }
        else {
            return mc.cameraEntity.getCameraPosVec(1);
        }
    }
    
    private List<Portal> getPortalsNearbySorted() {
        Vec3d cameraPos = mc.cameraEntity.getPos();
        double range = 128.0;
        return CHelper.getClientNearbyPortals(range)
            .sorted(
                Comparator.comparing(portalEntity ->
                    portalEntity.getDistanceToNearestPointInPortal(cameraPos)
                )
            ).collect(Collectors.toList());
    }
    
    protected abstract void doRenderPortal(
        Portal portal,
        MatrixStack matrixStack
    );
    
    protected final void manageCameraAndRenderPortalContent(
        Portal portal
    ) {
        if (getPortalLayer() > maxPortalLayer.get()) {
            return;
        }
        
        Entity cameraEntity = mc.cameraEntity;
        Camera camera = mc.gameRenderer.getCamera();
    
        if (getPortalLayer() >= 2 &&
            portal.getDistanceToNearestPointInPortal(cameraEntity.getPos()) >
                (16 * maxPortalLayer.get())
        ) {
            return;
        }
    
        MyRenderHelper.onBeginPortalWorldRendering(portalLayers);
        
        
        assert cameraEntity.world == mc.world;
        
        Vec3d oldPos = cameraEntity.getPos();
        Vec3d oldLastTickPos = McHelper.lastTickPosOf(cameraEntity);
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
        
        GlStateManager.enableDepthTest();
        GlStateManager.disableBlend();
        MyRenderHelper.restoreViewPort();
    
        CGlobal.myGameRenderer.resetFog();
    }
    
    protected void renderPortalContentWithContextSwitched(
        Portal portal, Vec3d oldCameraPos
    ) {
        GlStateManager.enableAlphaTest();
        GlStateManager.enableCull();
        
        WorldRenderer worldRenderer = CGlobal.clientWorldLoader.getWorldRenderer(portal.dimensionTo);
        ClientWorld destClientWorld = CGlobal.clientWorldLoader.getOrCreateFakedWorld(portal.dimensionTo);
        
        CHelper.checkGlError();
        
        CGlobal.myGameRenderer.renderWorld(
            MyRenderHelper.partialTicks, worldRenderer, destClientWorld, oldCameraPos
        );
    
        CHelper.checkGlError();
        
    }
}
