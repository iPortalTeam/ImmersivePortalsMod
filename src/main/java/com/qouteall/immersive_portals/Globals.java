package com.qouteall.immersive_portals;

import com.qouteall.immersive_portals.client_world_management.ClientWorldLoader;
import com.qouteall.immersive_portals.render.PortalRenderManager;
import com.qouteall.immersive_portals.render.ShaderManager;
import com.qouteall.immersive_portals.world_syncing.ChunkSyncingManager;

public class Globals {
    public static PortalRenderManager portalRenderManager;
    
    public static ShaderManager shaderManager;
    
    public static ClientWorldLoader clientWorldLoader;
    
    public static ChunkSyncingManager chunkSyncingManager;
}
