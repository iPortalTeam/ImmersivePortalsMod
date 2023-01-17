package qouteall.imm_ptl.core.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
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

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

public abstract class PortalRenderer {
    
    public static final Minecraft client = Minecraft.getInstance();
    
    public abstract void onBeforeTranslucentRendering(PoseStack matrixStack);
    
    public abstract void onAfterTranslucentRendering(PoseStack matrixStack);
    
    // will be called when rendering portal
    public abstract void onHandRenderingEnded(PoseStack matrixStack);
    
    // will be called when rendering portal
    public void onBeforeHandRendering(PoseStack matrixStack) {}
    
    // this will NOT be called when rendering portal
    public abstract void prepareRendering();
    
    // this will NOT be called when rendering portal
    public abstract void finishRendering();
    
    // this will be called when rendering portal entities
    public abstract void renderPortalInEntityRenderer(Portal portal);
    
    // return true to skip framebuffer clear
    // this will also be called in outer world rendering
    public abstract boolean replaceFrameBufferClearing();
    
    protected List<PortalLike> getPortalsToRender(PoseStack matrixStack) {
        Supplier<Frustum> frustumSupplier = Helper.cached(() -> {
            Frustum frustum = new Frustum(
                matrixStack.last().pose(),
                RenderSystem.getProjectionMatrix()
            );
            
            Vec3 cameraPos = client.gameRenderer.getMainCamera().getPosition();
            frustum.prepare(cameraPos.x, cameraPos.y, cameraPos.z);
            
            return frustum;
        });
        
        List<PortalLike> portalsToRender = new ArrayList<>();
        List<Portal> globalPortals = GlobalPortalStorage.getGlobalPortals(client.level);
        for (Portal globalPortal : globalPortals) {
            if (!shouldSkipRenderingPortal(globalPortal, frustumSupplier)) {
                portalsToRender.add(globalPortal);
            }
        }
        
        client.level.entitiesForRendering().forEach(e -> {
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
        
        Vec3 cameraPos = CHelper.getCurrentCameraPos();
        portalsToRender.sort(Comparator.comparingDouble(portalEntity ->
            portalEntity.getDistanceToNearestPointInPortal(cameraPos)
        ));
        return portalsToRender;
    }
    
    private static boolean shouldSkipRenderingPortal(Portal portal, Supplier<Frustum> frustumSupplier) {
        if (!portal.isPortalValid()) {
            return true;
        }
        
        // if max portal layer is 0, the invisible portals will be force rendered
        if (!portal.isVisible() && IPGlobal.maxPortalLayer != 0) {
            return true;
        }
        
        if (RenderStates.getRenderedPortalNum() >= IPGlobal.portalRenderLimit) {
            return true;
        }
        
        Vec3 cameraPos = TransformationManager.getIsometricAdjustedCameraPos();
        
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
    
    public static double getRenderRange() {
        double range = client.options.getEffectiveRenderDistance() * 16;
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
    
    protected final void renderPortalContent(
        PortalLike portal
    ) {
        if (PortalRendering.getPortalLayer() > PortalRendering.getMaxPortalLayer()) {
            return;
        }
        
        ClientLevel newWorld = ClientWorldLoader.getWorld(portal.getDestDim());
        
        PortalRendering.onBeginPortalWorldRendering();
        
        int renderDistance = getPortalRenderDistance(portal);
        
        invokeWorldRendering(new WorldRenderInfo(
            newWorld,
            PortalRendering.getRenderingCameraPos(),
            portal.getAdditionalCameraTransformation(),
            false, portal.getDiscriminator(),
            renderDistance,
            false,
            true
        ));
        
        PortalRendering.onEndPortalWorldRendering();
        
        GlStateManager._enableDepthTest();
        
        MyRenderHelper.restoreViewPort();
        
        
    }
    
    private static int getPortalRenderDistance(PortalLike portal) {
        int mcRenderDistance = client.options.getEffectiveRenderDistance();
        
        if (portal.getScale() > 2) {
            double radiusBlocks = portal.getDestAreaRadiusEstimation() * 1.4;
            
            radiusBlocks = Math.min(radiusBlocks, 32 * 16);
            
            return Math.max((int) (radiusBlocks / 16), mcRenderDistance);
        }
        if (IPGlobal.reducedPortalRendering) {
            return mcRenderDistance / 3;
        }
        return mcRenderDistance;
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
        if (portal.getRotation() == null) {
            return null;
        }
        
        Quaternionf rot = portal.getRotation().toMcQuaternion();
        rot.conjugate();
        return rot.get(new Matrix4f());
    }
    
    @Nullable
    public static Matrix4f combineNullable(@Nullable Matrix4f a, @Nullable Matrix4f b) {
        return Helper.combineNullable(a, b, (m1, m2) -> {
            m1.mul(m2);
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
            return new Matrix4f().scale(v, v, v);
        }
        return null;
    }
    
    public static boolean shouldApplyScaleToModelView(PortalLike portal) {
        return portal.hasScaling() && portal.isFuseView();
    }
    
    public void onBeginIrisTranslucentRendering(PoseStack matrixStack) {}
    
}
