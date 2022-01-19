package qouteall.imm_ptl.core.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
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
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.imm_ptl.core.IPCGlobal;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.portal.Mirror;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalLike;
import qouteall.imm_ptl.core.portal.global_portals.GlobalPortalStorage;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;
import qouteall.imm_ptl.core.render.context_management.RenderStates;
import qouteall.imm_ptl.core.render.context_management.WorldRenderInfo;
import qouteall.q_misc_util.Helper;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

public abstract class PortalRenderer {
    
    public static final MinecraftClient client = MinecraftClient.getInstance();
    
    public abstract void onBeforeTranslucentRendering(MatrixStack matrixStack);
    
    public abstract void onAfterTranslucentRendering(MatrixStack matrixStack);
    
    // will be called when rendering portal
    public abstract void onHandRenderingEnded(MatrixStack matrixStack);
    
    // will be called when rendering portal
    public void onBeforeHandRendering(MatrixStack matrixStack) {}
    
    // this will NOT be called when rendering portal
    public abstract void prepareRendering();
    
    // this will NOT be called when rendering portal
    public abstract void finishRendering();
    
    // this will be called when rendering portal entities
    public abstract void renderPortalInEntityRenderer(Portal portal);
    
    // return true to skip framebuffer clear
    // this will also be called in outer world rendering
    public abstract boolean replaceFrameBufferClearing();
    
    protected final void renderPortals(MatrixStack matrixStack) {
        Validate.isTrue(client.cameraEntity.world == client.world);
        
        Supplier<Frustum> frustumSupplier = Helper.cached(() -> {
            Frustum frustum = new Frustum(
                matrixStack.peek().getPositionMatrix(),
                RenderSystem.getProjectionMatrix()
            );
            
            Vec3d cameraPos = client.gameRenderer.getCamera().getPos();
            frustum.setPosition(cameraPos.x, cameraPos.y, cameraPos.z);
            
            return frustum;
        });
        
        List<PortalLike> portalsToRender = new ArrayList<>();
        List<Portal> globalPortals = GlobalPortalStorage.getGlobalPortals(client.world);
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
                    
                    if (renderingDelegate != portal) {
                        // a portal rendering group
                        if (!portalsToRender.contains(renderingDelegate)) {
                            portalsToRender.add(renderingDelegate);
                        }
                    }
                    else {
                        // a normal portal
                        portalsToRender.add(renderingDelegate);
                    }
                }
            }
        });
        
        Vec3d cameraPos = CHelper.getCurrentCameraPos();
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
        
        if (RenderStates.getRenderedPortalNum() >= IPGlobal.portalRenderLimit) {
            return true;
        }
        
        Vec3d cameraPos = TransformationManager.getIsometricAdjustedCameraPos();
        
        if (!portal.isRoughlyVisibleTo(cameraPos)) {
            return true;
        }
        
        if (PortalRendering.isRendering()) {
            PortalLike outerPortal = PortalRendering.getRenderingPortal();
            
            if (outerPortal.cannotRenderInMe(portal)) {
                return true;
            }
        }
        
        double distance = portal.getDistanceToNearestPointInPortal(cameraPos);
        if (distance > getRenderRange()) {
            return true;
        }
        
        if (IPCGlobal.earlyFrustumCullingPortal) {
            // frustum culling does not work when portal is very close
            if (distance > 0.03) {
                Frustum frustum = frustumSupplier.get();
                if (!frustum.isVisible(portal.getExactAreaBox())) {
                    return true;
                }
            }
        }
        return false;
    }
    
    protected final double getRenderRange() {
        double range = client.options.viewDistance * 16;
        if (RenderStates.isLaggy || IPGlobal.reducedPortalRendering) {
            range = 16;
        }
        if (PortalRendering.getPortalLayer() > 1) {
            //do not render deep layers of mirror when far away
            range /= (PortalRendering.getPortalLayer());
        }
        if (PortalRendering.getPortalLayer() >= 1) {
            double outerPortalScale = PortalRendering.getRenderingPortal().getScale();
            if (outerPortalScale > 2) {
                range *= outerPortalScale;
                range = Math.min(range, 32 * 16);
            }
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
        
        invokeWorldRendering(new WorldRenderInfo(
            newWorld,
            PortalRendering.getRenderingCameraPos(),
            portal.getAdditionalCameraTransformation(),
            false, portal.getDiscriminator(),
            renderDistance
        ));
        
        PortalRendering.onEndPortalWorldRendering();
        
        GlStateManager._enableDepthTest();
        
        MyRenderHelper.restoreViewPort();
        
        
    }
    
    private static int getPortalRenderDistance(PortalLike portal) {
        if (portal.getScale() > 2) {
            double radiusBlocks = portal.getDestAreaRadiusEstimation() * 1.4;
            
            radiusBlocks = Math.min(radiusBlocks, 32 * 16);
            
            return Math.max((int) (radiusBlocks / 16), client.options.viewDistance);
        }
        if (IPGlobal.reducedPortalRendering) {
            return client.options.viewDistance / 3;
        }
        return client.options.viewDistance;
    }
    
    public void invokeWorldRendering(
        WorldRenderInfo worldRenderInfo
    ) {
        MyGameRenderer.renderWorldNew(
            worldRenderInfo,
            Runnable::run
        );
    }
    
    @Nullable
    public static Matrix4f getPortalTransformation(Portal portal) {
        Matrix4f rot = getPortalRotationMatrix(portal);
        
        Matrix4f mirror = portal instanceof Mirror ?
            TransformationManager.getMirrorTransformation(portal.getNormal()) : null;
        
        Matrix4f scale = getPortalScaleMatrix(portal);
        
        return combineNullable(rot, combineNullable(mirror, scale));
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
        return Helper.combineNullable(a, b, (m1, m2) -> {
            m1.multiply(m2);
            return m1;
        });
    }
    
    @Nullable
    public static Matrix4f getPortalScaleMatrix(Portal portal) {
        // if it's not a fuseView portal
        // whether to apply scale transformation to camera does not change triangle position
        // to avoid abrupt fog change, do not apply for non-fuse-view portal
        // for fuse-view portal, the depth value should be correct so the scale should be applied
        if (shouldApplyScaleToModelView(portal)) {
            float v = (float) (1.0 / portal.getScale());
            return Matrix4f.scale(v, v, v);
        }
        return null;
    }
    
    public static boolean shouldApplyScaleToModelView(PortalLike portal) {
        return portal.hasScaling() && portal.isFuseView();
    }
    
}
