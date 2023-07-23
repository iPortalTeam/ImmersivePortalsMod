package qouteall.imm_ptl.core;

import net.minecraft.world.level.dimension.DimensionType;
import qouteall.imm_ptl.core.render.PortalRenderer;
import qouteall.imm_ptl.core.render.RendererDebug;
import qouteall.imm_ptl.core.render.RendererDummy;
import qouteall.imm_ptl.core.render.RendererUsingFrameBuffer;
import qouteall.imm_ptl.core.render.RendererUsingStencil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class IPCGlobal {
    
    public static PortalRenderer renderer;
    public static RendererUsingStencil rendererUsingStencil;
    public static RendererUsingFrameBuffer rendererUsingFrameBuffer;
    public static RendererDummy rendererDummy = new RendererDummy();
    public static RendererDebug rendererDebug = new RendererDebug();
    
    public static int maxIdleChunkRendererNum = 500;
    
    public static Map<DimensionType, Integer> renderInfoNumMap = new ConcurrentHashMap<>();
    
    public static boolean doUseAdvancedFrustumCulling = true;
    public static boolean useHackedChunkRenderDispatcher = true;
    public static boolean isClientRemoteTickingEnabled = true;
    public static boolean useFrontClipping = true;
    public static boolean doDisableAlphaTestWhenRenderingFrameBuffer = true;
    public static boolean lateClientLightUpdate = true;
    public static boolean earlyRemoteUpload = true;
    
    public static boolean useSuperAdvancedFrustumCulling = true;
    public static boolean earlyFrustumCullingPortal = true;
    
    public static boolean useSeparatedStencilFormat = false;
    
    public static boolean experimentalIrisPortalRenderer = false;
    
    public static boolean debugEnableStencilWithIris = false;
}
