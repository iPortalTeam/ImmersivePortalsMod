package qouteall.imm_ptl.core.render.context_management;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.Vec3d;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.portal.Mirror;
import qouteall.imm_ptl.core.portal.PortalLike;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Stack;
import java.util.function.Function;
import java.util.stream.Collectors;

// TODO remove this and use RenderInfo
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
        
        int number = 0;
        for (PortalLike portal : portalLayers) {
            if (portal instanceof Mirror) {
                number++;
            }
        }
        isRenderingOddNumberOfMirrorsCache = (number % 2 == 1);
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
    
    public static Vec3d getRenderingCameraPos() {
        Vec3d pos = RenderStates.originalCamera.getPos();
        for (PortalLike portal : portalLayers) {
            pos = portal.transformPoint(pos);
        }
        return pos;
    }
    
    public static double getAllScaling() {
        double scale = 1.0;
        for (PortalLike portal : portalLayers) {
            scale *= portal.getScale();
        }
        return scale;
    }
    
}
