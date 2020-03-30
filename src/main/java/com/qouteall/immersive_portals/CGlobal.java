package com.qouteall.immersive_portals;

import com.qouteall.immersive_portals.render.PortalRenderer;
import com.qouteall.immersive_portals.render.RendererDebug;
import com.qouteall.immersive_portals.render.RendererDummy;
import com.qouteall.immersive_portals.render.RendererUsingFrameBuffer;
import com.qouteall.immersive_portals.render.RendererUsingStencil;
import com.qouteall.immersive_portals.render.ShaderManager;
import com.qouteall.immersive_portals.teleportation.ClientTeleportationManager;
import net.minecraft.world.dimension.DimensionType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CGlobal {
    
    public static PortalRenderer renderer;
    public static RendererUsingStencil rendererUsingStencil;
    public static RendererUsingFrameBuffer rendererUsingFrameBuffer;
    public static RendererDummy rendererDummy = new RendererDummy();
    public static RendererDebug rendererDebug = new RendererDebug();
    
    public static ClientWorldLoader clientWorldLoader;
    public static ClientTeleportationManager clientTeleportationManager;
    public static ShaderManager shaderManager;
    
    public static int maxIdleChunkRendererNum = 500;
    
    public static Map<DimensionType, Integer> renderInfoNumMap = new ConcurrentHashMap<>();
    
    public static boolean doUseAdvancedFrustumCulling = true;
    public static boolean useHackedChunkRenderDispatcher = true;
    public static boolean isClientRemoteTickingEnabled = true;
    public static boolean useFrontCulling = true;
    public static boolean doDisableAlphaTestWhenRenderingFrameBuffer = true;
    public static boolean renderFewerInFastGraphic = true;
    public static boolean smoothChunkUnload = true;
    public static boolean earlyClientLightUpdate = true;
    public static boolean useSuperAdvancedFrustumCulling = true;
}
