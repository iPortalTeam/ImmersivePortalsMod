package com.qouteall.immersive_portals;

import com.qouteall.immersive_portals.chunk_loading.ChunkDataSyncManager;
import com.qouteall.immersive_portals.chunk_loading.ChunkTracker;
import com.qouteall.immersive_portals.render.MyGameRenderer;
import com.qouteall.immersive_portals.render.PortalRenderManager;
import com.qouteall.immersive_portals.teleportation.ClientTeleportationManager;
import com.qouteall.immersive_portals.teleportation.ServerTeleportationManager;

public class Globals {
    public static PortalRenderManager portalRenderManager;
    
    public static ClientWorldLoader clientWorldLoader;
    
    public static ChunkTracker chunkTracker;
    
    public static ChunkDataSyncManager chunkDataSyncManager;
    
    public static MyGameRenderer myGameRenderer;
    
    public static ServerTeleportationManager serverTeleportationManager;
    
    public static ClientTeleportationManager clientTeleportationManager;
    
    public static boolean doUseAdvancedFrustumCulling = true;
    public static int maxPortalLayer = 3;
    public static int maxIdleChunkRendererNum = 1000;
    public static Object switchedFogRenderer;
    public static boolean useHackedChunkRenderDispatcher = true;
    public static boolean isClientRemoteTickingEnabled = true;
    public static boolean isChunkLoadingMultiThreaded = true;
    public static boolean isOptifinePresent = false;
    public static boolean renderPortalBeforeTranslucentBlocks = true;
}
