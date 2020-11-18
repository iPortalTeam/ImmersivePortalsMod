package com.qouteall.immersive_portals.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.ClientWorldLoader;
import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.portal.Mirror;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.portal.PortalLike;
import com.qouteall.immersive_portals.render.context_management.PortalRendering;
import com.qouteall.immersive_portals.render.context_management.RenderStates;
import com.qouteall.immersive_portals.render.context_management.RenderingHierarchy;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

public abstract class PortalRenderer {
    
    public static final MinecraftClient client = MinecraftClient.getInstance();
    
    
    // this WILL be called when rendering portal
    public abstract void onBeforeTranslucentRendering(MatrixStack matrixStack);
    
    // this WILL be called when rendering portal
    public abstract void onAfterTranslucentRendering(MatrixStack matrixStack);
    
    // this WILL be called when rendering portal
    public abstract void onRenderCenterEnded(MatrixStack matrixStack);
    
    // this will NOT be called when rendering portal
    public abstract void prepareRendering();
    
    // this will NOT be called when rendering portal
    public abstract void finishRendering();
    
    // this will be called when rendering portal entities
    public abstract void renderPortalInEntityRenderer(Portal portal);
    
    // return true to skip framebuffer clear
    // this will also be called in outer world rendering
    public abstract boolean replaceFrameBufferClearing();
    
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
        
        List<PortalLike> portalsToRender = new ArrayList<>();
        List<Portal> globalPortals = McHelper.getGlobalPortals(client.world);
        for (Portal globalPortal : globalPortals) {
            if (!shouldSkipRenderingPortal(globalPortal, frustumSupplier)) {
                portalsToRender.add(globalPortal);
            }
        }
        
        client.world.getEntities().forEach(e -> {
            if (e instanceof Portal) {
                Portal portal = (Portal) e;
                if (!shouldSkipRenderingPortal(portal, frustumSupplier)) {
                    
                    PortalLike renderingDelegate = portal.getRenderingDelegate();
                    
                    // this is O(n^2) but not hot spot
                    if (!portalsToRender.contains(renderingDelegate)) {
                        portalsToRender.add(renderingDelegate);
                    }
                }
            }
        });
        
        Vec3d cameraPos = McHelper.getCurrentCameraPos();
        portalsToRender.sort(Comparator.comparingDouble(portalEntity ->
            portalEntity.getDistanceToNearestPointInPortal(cameraPos)
        ));
        
        for (PortalLike portal : portalsToRender) {
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
        
        if (!portal.isRoughlyVisibleTo(cameraPos)) {
            return true;
        }
        
        if (PortalRendering.isRendering()) {
            PortalLike outerPortal = PortalRendering.getRenderingPortal();
            
            if (outerPortal.isParallelWith(portal)) {
                return true;
            }
        }
        
        if (isOutOfDistance(portal)) {
            return true;
        }
        
        if (CGlobal.earlyFrustumCullingPortal) {
            Frustum frustum = frustumSupplier.get();
            if (!frustum.isVisible(portal.getExactAreaBox())) {
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
            range *= PortalRendering.getRenderingPortal().getScale();
            range = Math.min(range, 32 * 16);
        }
        return range;
    }
    
    protected abstract void doRenderPortal(
        PortalLike portal,
        MatrixStack matrixStack
    );
    
    protected final void renderPortalContent(
        PortalLike portal
    ) {
        if (PortalRendering.getPortalLayer() > PortalRendering.getMaxPortalLayer()) {
            return;
        }
        
        Entity cameraEntity = client.cameraEntity;
        
        ClientWorld newWorld = ClientWorldLoader.getWorld(portal.getDestDim());
        
        Camera camera = client.gameRenderer.getCamera();
        
        PortalRendering.onBeginPortalWorldRendering();
        
        int renderDistance = getPortalRenderDistance(portal);
        
        invokeWorldRendering(new RenderingHierarchy(
            newWorld,
            PortalRendering.getRenderingCameraPos(),
            portal.getAdditionalCameraTransformation(),
            portal.getDiscriminator(),
            renderDistance
        ));
        
        PortalRendering.onEndPortalWorldRendering();
        
        GlStateManager.enableDepthTest();
        GlStateManager.disableBlend();
        
        MyRenderHelper.restoreViewPort();
        
        
    }
    
    private static int getPortalRenderDistance(PortalLike portal) {
        if (portal.getScale() > 2) {
            double radiusBlocks = portal.getDestAreaRadiusEstimation() * 1.4;
            
            radiusBlocks = Math.min(radiusBlocks, 32 * 16);
            
            return Math.max((int) (radiusBlocks / 16), client.options.viewDistance);
        }
        if (Global.reducedPortalRendering) {
            return client.options.viewDistance / 3;
        }
        return client.options.viewDistance;
    }
    
    public void invokeWorldRendering(
        RenderingHierarchy renderingHierarchy
    ) {
        MyGameRenderer.renderWorldNew(
            renderingHierarchy,
            Runnable::run
        );
    }
    
    private boolean isOutOfDistance(PortalLike portal) {
        
        Vec3d cameraPos = McHelper.getCurrentCameraPos();
        if (portal.getDistanceToNearestPointInPortal(cameraPos) > getRenderRange()) {
            return true;
        }
        
        return false;
    }
    
    @Nullable
    public static Matrix4f getPortalTransformation(Portal portal) {
        Matrix4f rot = getPortalRotationMatrix(portal);
        
        Matrix4f mirror = portal instanceof Mirror ?
            TransformationManager.getMirrorTransformation(portal.getNormal()) : null;
        
        return combineNullable(rot, mirror);
    }
    
    @Nullable
    public static Matrix4f getPortalRotationMatrix(Portal portal) {
        if (portal.rotation == null) {
            return null;
        }
        
        Quaternion rot = portal.rotation.copy();
        rot.conjugate();
        return new Matrix4f(rot);
    }
    
    @Nullable
    public static Matrix4f combineNullable(@Nullable Matrix4f a, @Nullable Matrix4f b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        a.multiply(b);
        return a;
    }
    
}
