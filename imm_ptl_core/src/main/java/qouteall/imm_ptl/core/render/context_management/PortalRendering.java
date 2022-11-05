package qouteall.imm_ptl.core.render.context_management;

import net.coderbot.iris.shadows.frustum.advanced.AdvancedShadowCullingFrustum;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.Validate;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.ducks.IEFrustum;
import qouteall.imm_ptl.core.ducks.IEWorldRenderer;
import qouteall.imm_ptl.core.portal.Mirror;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalLike;
import qouteall.imm_ptl.core.render.PortalRenderer;

import javax.annotation.Nullable;
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
     * @return The cave culling starting point. Null if it's a portal group (e.g. a scale box).
     */
    @Nullable
    public static BlockPos getCaveCullingStartingPoint() {
        Validate.isTrue(isRendering());
        PortalLike renderingPortal = getRenderingPortal();
        
        if (!(renderingPortal instanceof Portal portal)) {
            return null;
        }
        
        Minecraft mc = Minecraft.getInstance();
        Vec3 cameraPos = CHelper.getCurrentCameraPos();
        
        Vec3 outerCameraPos = portal.inverseTransformPoint(cameraPos);
        Vec3 nearestPoint = portal.getNearestPointInPortal(outerCameraPos);
        
        // for dimension stack, don't make it to be inside other side's block.
        // move it backwards a little.
        nearestPoint = nearestPoint.add(portal.getNormal().scale(0.00001));
        
        Vec3 result = portal.transformPoint(nearestPoint);
        
        return new BlockPos(result);
    }
    
}
