package com.qouteall.immersive_portals;

import com.qouteall.immersive_portals.chunk_loading.ChunkDataSyncManager;
import com.qouteall.immersive_portals.chunk_loading.ChunkTracker;
import com.qouteall.immersive_portals.client_world_management.ClientWorldLoader;
import com.qouteall.immersive_portals.render.MyGameRenderer;
import com.qouteall.immersive_portals.render.PortalRenderManager;
import com.qouteall.immersive_portals.render.ShaderManager;
import com.qouteall.immersive_portals.teleportation.ClientTeleportationManager;
import com.qouteall.immersive_portals.teleportation.ServerTeleportationManager;

public class Globals {
    public static PortalRenderManager portalRenderManager;
    
    public static ShaderManager shaderManager;
    
    //TODO unload it at appropriate time
    public static ClientWorldLoader clientWorldLoader;
    
    public static ChunkTracker chunkTracker;
    
    public static ChunkDataSyncManager chunkDataSyncManager;
    
    public static MyGameRenderer myGameRenderer;
    
    public static ServerTeleportationManager serverTeleportationManager;
    
    public static ClientTeleportationManager clientTeleportationManager;
    
}
