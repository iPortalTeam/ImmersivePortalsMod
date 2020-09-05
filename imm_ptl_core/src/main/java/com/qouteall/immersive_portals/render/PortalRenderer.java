package com.qouteall.immersive_portals.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.CHelper;
import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
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
import java.util.function.Supplier;

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
        
        Supplier<Frustum> frustumSupplier = Helper.cached(() -> {
            Frustum frustum = new Frustum(
                matrixStack.peek().getModel(),
                RenderStates.projectionMatrix
            );
            
            Vec3d cameraPos = client.gameRenderer.getCamera().getPos();
            frustum.setPosition(cameraPos.x, cameraPos.y, cameraPos.z);
            
            return frustum;
        });
        
        List<Portal> portalsToRender = CHelper.getClientNearbyPortalList(
            getRenderRange(),
            portal -> !shouldSkipRenderingPortal(portal, frustumSupplier)
        );
        
        Vec3d cameraPos = McHelper.getCurrentCameraPos();
        portalsToRender.sort(Comparator.comparingDouble(portalEntity ->
            portalEntity.getDistanceToNearestPointInPortal(cameraPos)
        ));
        
        for (Portal portal : portalsToRender) {
            doRenderPortal(portal, matrixStack);
        }
    }
    
    private boolean shouldSkipRenderingPortal(Portal portal, Supplier<Frustum> frustumSupplier) {
        if (!portal.isPortalValid()) {
            return true;
        }
        
        if (RenderStates.getRenderedPortalNum() >= Global.portalRenderLimit) {
            return true;
        }
        
        Vec3d cameraPos = McHelper.getCurrentCameraPos();
        
        if (!portal.isInFrontOfPortal(cameraPos)) {
            return true;
        }
        
        if (PortalRendering.isRendering()) {
            Portal outerPortal = PortalRendering.getRenderingPortal();
            if (Portal.isParallelOrientedPortal(portal, outerPortal)) {
                return true;
            }
        }
        
        if (isOutOfDistance(portal)) {
            return true;
        }
        
        if (CGlobal.earlyFrustumCullingPortal) {
            Frustum frustum = frustumSupplier.get();
            if (!frustum.isVisible(portal.getBoundingBox())) {
                return true;
            }
        }
        return false;
    }
    
    protected final double getRenderRange() {
        double range = client.options.viewDistance * 16;
        if (RenderStates.isLaggy || Global.reducedPortalRendering) {
            range = 16;
        }
        if (PortalRendering.getPortalLayer() > 1) {
            //do not render deep layers of mirror when far away
            range /= (PortalRendering.getPortalLayer());
        }
        if (PortalRendering.getPortalLayer() >= 1) {
            range *= PortalRendering.getRenderingPortal().scaling;
        }
        return range;
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
        
        ClientWorld newWorld = CGlobal.clientWorldLoader.getWorld(portal.dimensionTo);
        
        Camera camera = client.gameRenderer.getCamera();
        
        PortalRendering.onBeginPortalWorldRendering();
        
        int renderDistance = client.options.viewDistance;
        if (portal.scaling > 2) {
            renderDistance *= (int) (Math.min(portal.scaling, 4));
        }
        
        invokeWorldRendering(new RenderInfo(
            newWorld,
            PortalRendering.getRenderingCameraPos(),
            getAdditionalCameraTransformation(portal),
            portal,
            renderDistance
        ));
        
        PortalRendering.onEndPortalWorldRendering();
        
        GlStateManager.enableDepthTest();
        GlStateManager.disableBlend();
        
        MyRenderHelper.restoreViewPort();
        
        
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
        
        Vec3d cameraPos = McHelper.getCurrentCameraPos();
        if (portal.getDistanceToNearestPointInPortal(cameraPos) > getRenderRange()) {
            return true;
        }
        
        return false;
    }
    
    // Scaling does not interfere camera transformation
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
