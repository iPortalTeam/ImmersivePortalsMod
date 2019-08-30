package com.qouteall.immersive_portals;

import com.qouteall.immersive_portals.chunk_loading.ChunkDataSyncManager;
import com.qouteall.immersive_portals.chunk_loading.ChunkTracker;
import com.qouteall.immersive_portals.teleportation.ServerTeleportationManager;

public class SGlobal {
    
    public static ChunkTracker chunkTracker;
    
    public static ChunkDataSyncManager chunkDataSyncManager;
    
    public static ServerTeleportationManager serverTeleportationManager;
    
    public static boolean isChunkLoadingMultiThreaded = true;
}
