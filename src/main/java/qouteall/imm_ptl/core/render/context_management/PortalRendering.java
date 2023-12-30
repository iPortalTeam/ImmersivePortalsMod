package qouteall.imm_ptl.core.render.context_management;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.portal.Mirror;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalLike;
import qouteall.imm_ptl.core.portal.shape.BoxPortalShape;
import qouteall.imm_ptl.core.render.VisibleSectionDiscovery;
import qouteall.imm_ptl.core.render.renderer.PortalRenderer;
import qouteall.q_misc_util.my_util.Plane;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Stack;
import java.util.function.Function;
import java.util.stream.Collectors;

@Environment(EnvType.CLIENT)
public class PortalRendering {
    private static final Stack<PortalLike> portalLayers = new Stack<>();
    private static boolean isRenderingCache = false;
    private static boolean isRenderingOddNumberOfMirrorsCache = false;
    
    public static void pushPortalLayer(PortalLike portal) {
        portalLayers.push(portal);
        updateCache();
    }
    
    public static void popPortalLayer() {
        portalLayers.pop();
        updateCache();
    }
    
    private static void updateCache() {
        isRenderingCache = getPortalLayer() != 0;
        
        int mirrorNum = 0;
        for (PortalLike portal : portalLayers) {
            if (portal instanceof Mirror) {
                mirrorNum++;
            }
        }
        isRenderingOddNumberOfMirrorsCache = (mirrorNum % 2 == 1);
    }
    
    //0 for rendering outer world
    //1 for rendering world inside portal
    //2 for rendering world inside the portal inside portal
    public static int getPortalLayer() {
        return portalLayers.size();
    }
    
    public static boolean isRendering() {
        return isRenderingCache;
    }
    
    public static boolean isRenderingOddNumberOfMirrors() {
        return isRenderingOddNumberOfMirrorsCache;
    }
    
    public static int getMaxPortalLayer() {
        if (RenderStates.isLaggy) {
            return 1;
        }
        return IPGlobal.maxPortalLayer;
    }
    
    /**
     * @return The innermost portal that's currently being rendered.
     * Must use after checking {@link #isRendering()}
     */
    public static PortalLike getRenderingPortal() {
        return portalLayers.peek();
    }
    
    public static void onBeginPortalWorldRendering() {
        List<WeakReference<PortalLike>> currRenderInfo = portalLayers.stream().map(
            (Function<PortalLike, WeakReference<PortalLike>>) WeakReference::new
        ).collect(Collectors.toList());
        RenderStates.portalRenderInfos.add(currRenderInfo);
        RenderStates.portalsRenderedThisFrame++;
        
        if (portalLayers.stream().anyMatch(PortalLike::hasScaling)) {
            RenderStates.renderedScalingPortal = true;
        }
        
        CHelper.checkGlError();
    }
    
    public static void onEndPortalWorldRendering() {
        RenderStates.renderedDimensions.add(
            portalLayers.peek().getDestDim()
        );
    }
    
    public static Vec3 getRenderingCameraPos() {
        Vec3 pos = RenderStates.originalCamera.getPosition();
        for (PortalLike portal : portalLayers) {
            pos = portal.transformPoint(pos);
        }
        return pos;
    }
    
    public static double getExtraModelViewScaling() {
        double scale = 1.0;
        for (PortalLike portal : portalLayers) {
            if (!PortalRenderer.shouldApplyScaleToModelView(portal)) {
                scale *= portal.getScale();
            }
        }
        return scale;
    }
    
    /**
     * This only has effects with Sodium. In vanilla it uses {@link VisibleSectionDiscovery}.
     * <br>
     * As I tested, cave culling can optimize 20% when you are very close to the portal.
     * But its optimization is negligible when you are 5 blocks from the portal.
     * <br>
     * And if something is behind the portal destination, cave culling may cull wrongly.
     * If the camera is close to the portal, having wrong culling is nearly impossible.
     * <br>
     * So the conclusion is to only enable cave culling when close to the portal.
     */
    public static boolean shouldEnableSodiumCaveCulling() {
        if (isRendering()) {
            PortalLike renderingPortal = getRenderingPortal();
            
            if (renderingPortal instanceof Portal portal) {
                if (portal.getPortalShape() instanceof BoxPortalShape) {
                    return false;
                }
            }
            
            Vec3 currentCameraPos = CHelper.getCurrentCameraPos();
            Vec3 originalCameraPos = renderingPortal.inverseTransformPoint(currentCameraPos);
            double distance = renderingPortal.getDistanceToNearestPointInPortal(originalCameraPos);
            return distance < 5;
        }
        return false;
    }
    
    // disable rendering hit result in mirror
    // because currently the block frame is not being culled
    // when pointing on the glass block or a mirror, it will wrongly render the block frame
    public static boolean shouldRenderHitResult() {
        if (isRendering()) {
            HitResult hitResult = Minecraft.getInstance().hitResult;
            if (hitResult == null) {
                return false;
            }
            
            PortalLike renderingPortal = getRenderingPortal();
            if (renderingPortal instanceof Mirror) {
                return false;
            }
            
            if ((hitResult instanceof BlockHitResult blockHitResult) && hitResult.getType() == HitResult.Type.BLOCK) {
                BlockPos blockPos = blockHitResult.getBlockPos();
                Plane innerClipping = renderingPortal.getInnerClipping();
                if (innerClipping != null) {
                    boolean roughlyInside = innerClipping.move(-0.1)
                        .isPointOnPositiveSide(Vec3.atCenterOf(blockPos));
                    return roughlyInside;
                }
            }
        }
        return true;
    }
    
    public static @Nullable Plane getActiveClippingPlane() {
        
        PortalLike renderingPortal = getRenderingPortal();
        
        Plane plane = renderingPortal.getInnerClipping();
        
        if (plane == null) {
            if (portalLayers.size() >= 2) {
                // It's a workaround for scale box rendering
                // The portal rendering group has no inner clipping
                // Then we need to inherit clipping plane from outer layers
                int i = portalLayers.size() - 2;
                while (i >= 0) {
                    PortalLike portal = portalLayers.get(i);
                    Plane outerPlane = portal.getInnerClipping();
                    
                    if (outerPlane != null) {
                        // transform that plane to the current rendering coordinate
                        // the inner clipping plane should be in the outermost portal's destination coordinate
                        for (int j = i + 1; j < portalLayers.size(); j++) {
                            PortalLike portal1 = portalLayers.get(j);
                            outerPlane = new Plane(
                                portal1.transformPoint(outerPlane.pos()),
                                portal1.transformLocalVecNonScale(outerPlane.normal())
                            );
                        }
                        
                        return outerPlane;
                    }
                    
                    i--;
                }
            }
        }
        
        return plane;
    }
    
    public static boolean isInvalidRecursionRendering(
        Portal toRender
    ) {
        if (portalLayers.size() < 2) {
            return false;
        }
        
        PortalLike last = portalLayers.get(portalLayers.size() - 1);
        PortalLike secondLast = portalLayers.get(portalLayers.size() - 2);
        
        if (last instanceof Portal lastPortal) {
            return toRender == secondLast && Portal.isReversePortal(toRender, lastPortal);
        }
        
        return false;
    }
}
