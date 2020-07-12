package com.qouteall.immersive_portals.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.CHelper;
import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.portal.Mirror;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.render.context_management.PortalRendering;
import com.qouteall.immersive_portals.render.context_management.RenderInfo;
import com.qouteall.immersive_portals.render.context_management.RenderStates;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.math.Vec3d;
import org.apache.commons.lang3.Validate;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public abstract class PortalRenderer {
    
    public static final MinecraftClient client = MinecraftClient.getInstance();
    
   
    
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
    
    public abstract boolean shouldSkipClearing();
    
    protected void renderPortals(MatrixStack matrixStack) {
        Validate.isTrue(client.cameraEntity.world == client.world);
        
        List<Portal> portalsNearbySorted = getPortalsNearbySorted();
        
        if (portalsNearbySorted.isEmpty()) {
            return;
        }
        
        Frustum frustum = null;
        if (CGlobal.earlyFrustumCullingPortal) {
            frustum = new Frustum(
                matrixStack.peek().getModel(),
                RenderStates.projectionMatrix
            );
            
            Vec3d cameraPos = client.gameRenderer.getCamera().getPos();
            frustum.setPosition(cameraPos.x, cameraPos.y, cameraPos.z);
        }
        
        for (Portal portal : portalsNearbySorted) {
            renderPortalIfRoughCheckPassed(portal, matrixStack, frustum);
        }
    }
    
    private void renderPortalIfRoughCheckPassed(
        Portal portal,
        MatrixStack matrixStack,
        Frustum frustum
    ) {
        if (!portal.isPortalValid()) {
            Helper.err("rendering invalid portal " + portal);
            return;
        }
        
        if (RenderStates.getRenderedPortalNum() >= Global.portalRenderLimit) {
            return;
        }
        
        Vec3d thisTickEyePos = client.gameRenderer.getCamera().getPos();
        
        if (!portal.isInFrontOfPortal(thisTickEyePos)) {
            return;
        }
        
        if (PortalRendering.isRendering()) {
            Portal outerPortal = PortalRendering.getRenderingPortal();
            if (Portal.isParallelPortal(portal, outerPortal)) {
                return;
            }
        }
        
        if (isOutOfDistance(portal)) {
            return;
        }
        
        if (CGlobal.earlyFrustumCullingPortal) {
            if (!frustum.isVisible(portal.getBoundingBox())) {
                return;
            }
        }
        
        doRenderPortal(portal, matrixStack);
    }
    
    protected final double getRenderRange() {
        double range = client.options.viewDistance * 16;
        if (PortalRendering.getPortalLayer() > 1) {
            //do not render deep layers of mirror when far away
            range /= (PortalRendering.getPortalLayer());
        }
        if (RenderStates.isLaggy) {
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
    
    protected final void renderPortalContent(
        Portal portal
    ) {
        if (PortalRendering.getPortalLayer() > PortalRendering.getMaxPortalLayer()) {
            return;
        }
        
        Entity cameraEntity = client.cameraEntity;
        
//        Vec3d newEyePos = portal.transformPoint(McHelper.getEyePos(cameraEntity));
//        Vec3d newLastTickEyePos = portal.transformPoint(McHelper.getLastTickEyePos(cameraEntity));
        
        ClientWorld newWorld = CGlobal.clientWorldLoader.getWorld(portal.dimensionTo);
        
        Camera camera = client.gameRenderer.getCamera();
        
        assert cameraEntity.world == client.world;
        
        PortalRendering.onBeginPortalWorldRendering();
        
        invokeWorldRendering(new RenderInfo(
            newWorld,
            PortalRendering.getRenderingCameraPos(),
            getAdditionalCameraTransformation(portal),
            portal
        ));
        
        PortalRendering.onEndPortalWorldRendering();
        
        GlStateManager.enableDepthTest();
        GlStateManager.disableBlend();
        
        MyRenderHelper.restoreViewPort();
        
        MyGameRenderer.updateFogColor();
        MyGameRenderer.resetFogState();
    }
    
    public void invokeWorldRendering(
        RenderInfo renderInfo
    ) {
        MyGameRenderer.renderWorldNew(
            renderInfo,
            Runnable::run
        );
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
    
    @Nullable
    public static Matrix4f getAdditionalCameraTransformation(Portal portal) {
        if (portal instanceof Mirror) {
            return TransformationManager.getMirrorTransformation(portal.getNormal());
        }
        else {
            if (portal.rotation != null) {
                Quaternion rot = portal.rotation.copy();
                rot.conjugate();
                return new Matrix4f(rot);
            }
            else {
                return null;
            }
        }
    }
    
}
