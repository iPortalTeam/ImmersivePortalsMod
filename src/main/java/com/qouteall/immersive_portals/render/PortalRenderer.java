package com.qouteall.immersive_portals.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.CHelper;
import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.OFInterface;
import com.qouteall.immersive_portals.portal.Mirror;
import com.qouteall.immersive_portals.portal.Portal;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.Matrix3f;
import net.minecraft.client.util.math.Matrix4f;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.dimension.DimensionType;

import java.util.Comparator;
import java.util.List;
import java.util.Stack;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public abstract class PortalRenderer {
    
    public static final MinecraftClient client = MinecraftClient.getInstance();
    protected Supplier<Integer> maxPortalLayer = () -> {
        if (MyRenderHelper.isLaggy) {
            return 1;
        }
        return Global.maxPortalLayer;
    };
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
        if (!Global.renderYourselfInPortal) {
            return false;
        }
        if (!isRendering()) {
            return false;
        }
        if (client.cameraEntity.dimension == MyRenderHelper.originalPlayerDimension) {
//            if (TransformationManager.isAnimationRunning()) {
//                return false;
//            }
            return true;
        }
        return false;
    }
    
    public boolean shouldRenderEntityNow(Entity entity) {
        if (OFInterface.isShadowPass.getAsBoolean()) {
            return true;
        }
        if (isRendering()) {
            if (entity instanceof ClientPlayerEntity) {
                return shouldRenderPlayerItself();
            }
            return getRenderingPortal().canRenderEntityInsideMe(
                entity.getCameraPosVec(1), -0.01
            );
        }
        return true;
    }
    
    protected void renderPortals(MatrixStack matrixStack) {
        assert client.cameraEntity.world == client.world;
        assert client.cameraEntity.dimension == client.world.dimension.getType();
        
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
    
        if (MyRenderHelper.getRenderedPortalNum() >= Global.portalRenderLimit) {
            return;
        }
        
        Vec3d thisTickEyePos = getRoughTestCameraPos();
        
        if (!portal.isInFrontOfPortal(thisTickEyePos)) {
            return;
        }
        
        if (isRendering()) {
            Portal outerPortal = portalLayers.peek();
            if (isReversePortal(portal, outerPortal)) {
                return;
            }
        }
        
        if (isOutOfDistance(portal)) {
            return;
        }
        
        doRenderPortal(portal, matrixStack);
    }
    
    private static boolean isReversePortal(Portal currPortal, Portal outerPortal) {
        if (currPortal.dimension != outerPortal.dimensionTo) {
            return false;
        }
        if (currPortal.dimensionTo != outerPortal.dimension) {
            return false;
        }
        if (currPortal.getNormal().dotProduct(outerPortal.getContentDirection()) > -0.9) {
            return false;
        }
        return !outerPortal.canRenderEntityInsideMe(currPortal.getPos(), 0.1);
    }
    
    private Vec3d getRoughTestCameraPos() {
        return client.gameRenderer.getCamera().getPos();
    }
    
    protected final double getRenderRange() {
        double range = client.options.viewDistance * 16;
        if (getPortalLayer() > 1) {
            //do not render deep layers of mirror when far away
            range /= (getPortalLayer());
        }
        if (MyRenderHelper.isLaggy) {
            range = 16;
        }
        return range;
    }
    
    private List<Portal> getPortalsNearbySorted() {
        Vec3d cameraPos = client.gameRenderer.getCamera().getPos();
        return CHelper.getClientNearbyPortals(getRenderRange())
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
        
        
        Entity cameraEntity = client.cameraEntity;
        
        MyRenderHelper.onBeginPortalWorldRendering(portalLayers);
        
        Camera camera = client.gameRenderer.getCamera();
        
        assert cameraEntity.world == client.world;
        
        Vec3d oldEyePos = McHelper.getEyePos(cameraEntity);
        Vec3d oldLastTickEyePos = McHelper.getLastTickEyePos(cameraEntity);
        DimensionType oldDimension = cameraEntity.dimension;
        ClientWorld oldWorld = ((ClientWorld) cameraEntity.world);
        
        Vec3d oldCameraPos = camera.getPos();
        
        Vec3d newEyePos = portal.transformPoint(oldEyePos);
        Vec3d newLastTickEyePos = portal.transformPoint(oldLastTickEyePos);
        DimensionType newDimension = portal.dimensionTo;
        ClientWorld newWorld =
            CGlobal.clientWorldLoader.getWorld(newDimension);
        //Vec3d newCameraPos = portal.applyTransformationToPoint(oldCameraPos);
        
        McHelper.setEyePos(cameraEntity, newEyePos, newLastTickEyePos);
        cameraEntity.dimension = newDimension;
        cameraEntity.world = newWorld;
        client.world = newWorld;
        
        renderPortalContentWithContextSwitched(
            portal, oldCameraPos, oldWorld
        );
        
        //restore the position
        cameraEntity.dimension = oldDimension;
        cameraEntity.world = oldWorld;
        client.world = oldWorld;
        McHelper.setEyePos(cameraEntity, oldEyePos, oldLastTickEyePos);
        
        GlStateManager.enableDepthTest();
        GlStateManager.disableBlend();
        MyRenderHelper.restoreViewPort();
        
        MyGameRenderer.updateFogColor();
        MyGameRenderer.resetFogState();
    }
    
    private boolean isOutOfDistance(Portal portal) {
        
        return false;
//        Vec3d cameraPos = client.gameRenderer.getCamera().getPos();
//        if (portal.getDistanceToNearestPointInPortal(cameraPos) > getRenderRange()) {
//            return true;
//        }
//
//        if (getPortalLayer() >= 1 &&
//            portal.getDistanceToNearestPointInPortal(cameraPos) >
//                (16 * maxPortalLayer.get())
//        ) {
//            return true;
//        }
//        return false;
    }
    
    protected void renderPortalContentWithContextSwitched(
        Portal portal, Vec3d oldCameraPos, ClientWorld oldWorld
    ) {
        GlStateManager.enableAlphaTest();
        GlStateManager.enableCull();
        
        WorldRenderer worldRenderer = CGlobal.clientWorldLoader.getWorldRenderer(portal.dimensionTo);
        ClientWorld destClientWorld = CGlobal.clientWorldLoader.getWorld(portal.dimensionTo);
        
        CHelper.checkGlError();
        
        MyGameRenderer.renderWorld(
            MyRenderHelper.tickDelta, worldRenderer, destClientWorld, oldCameraPos, oldWorld
        );
        
        CHelper.checkGlError();
        
    }
    
    public void applyAdditionalTransformations(MatrixStack matrixStack) {
        portalLayers.forEach(portal -> {
            if (portal instanceof Mirror) {
                Matrix4f matrix = TransformationManager.getMirrorTransformation(portal.getNormal());
                matrixStack.peek().getModel().multiply(matrix);
                matrixStack.peek().getNormal().multiply(new Matrix3f(matrix));
            }
            else if (portal.rotation != null) {
                Quaternion rot = portal.rotation.copy();
                rot.conjugate();
                matrixStack.multiply(rot);
            }
        });
    }
    
}
