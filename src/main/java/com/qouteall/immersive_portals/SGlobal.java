package com.qouteall.immersive_portals;

import com.qouteall.immersive_portals.chunk_loading.ChunkDataSyncManager;
import com.qouteall.immersive_portals.chunk_loading.ChunkTracker;
import com.qouteall.immersive_portals.teleportation.ServerTeleportationManager;
import net.minecraft.world.dimension.DimensionType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SGlobal {
    
    public static ChunkTracker chunkTracker;
    
    public static ChunkDataSyncManager chunkDataSyncManager;
    
    public static ServerTeleportationManager serverTeleportationManager;
    
    public static boolean doUseAdvancedFrustumCulling = true;
    public static int maxPortalLayer = 3;
    public static int maxIdleChunkRendererNum = 1000;
    public static Object switchedFogRenderer;
    public static boolean useHackedChunkRenderDispatcher = true;
    public static boolean isClientRemoteTickingEnabled = true;
    public static boolean isChunkLoadingMultiThreaded = true;
    public static boolean isOptifinePresent = false;
    public static boolean renderPortalBeforeTranslucentBlocks = true;
    public static boolean useFrontCulling = true;
    
    public static Map<DimensionType, Integer> renderInfoNumMap = new ConcurrentHashMap<>();
}
