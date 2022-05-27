package qouteall.imm_ptl.core;

import net.minecraft.world.level.dimension.DimensionType;
import qouteall.imm_ptl.core.render.PortalRenderer;
import qouteall.imm_ptl.core.render.RendererDebug;
import qouteall.imm_ptl.core.render.RendererDummy;
import qouteall.imm_ptl.core.render.RendererUsingFrameBuffer;
import qouteall.imm_ptl.core.render.RendererUsingStencil;
import qouteall.imm_ptl.core.teleportation.ClientTeleportationManager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class IPCGlobal {
    
    public static PortalRenderer renderer;
    public static RendererUsingStencil rendererUsingStencil;
    public static RendererUsingFrameBuffer rendererUsingFrameBuffer;
    public static RendererDummy rendererDummy = new RendererDummy();
    public static RendererDebug rendererDebug = new RendererDebug();
    
    public static ClientTeleportationManager clientTeleportationManager;
    
    public static int maxIdleChunkRendererNum = 500;
    
    public static Map<DimensionType, Integer> renderInfoNumMap = new ConcurrentHashMap<>();
    
    public static boolean doUseAdvancedFrustumCulling = true;
    public static boolean useHackedChunkRenderDispatcher = true;
    public static boolean isClientRemoteTickingEnabled = true;
    public static boolean useFrontClipping = true;
    public static boolean doDisableAlphaTestWhenRenderingFrameBuffer = true;
    // TODO remove this
    public static boolean smoothChunkUnload = true;
    public static boolean earlyClientLightUpdate = true;
    public static boolean earlyRemoteUpload = true;
    
    public static boolean useSuperAdvancedFrustumCulling = true;
    public static boolean earlyFrustumCullingPortal = true;
    
    public static enum StencilMode {
        none, packedWithDepth, separatedStencil
    }
    
    public static boolean useAnotherStencilFormat = false;
}
