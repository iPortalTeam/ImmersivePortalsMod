package com.qouteall.immersive_portals;

import com.qouteall.immersive_portals.client_world_management.ClientWorldLoader;
import com.qouteall.immersive_portals.render.MyGameRenderer;
import com.qouteall.immersive_portals.render.PortalRenderManager;
import com.qouteall.immersive_portals.render.ShaderManager;
import com.qouteall.immersive_portals.teleportation.ClientTeleportationManager;
import com.qouteall.immersive_portals.teleportation.ServerTeleportationManager;
import com.qouteall.immersive_portals.chunk_loading.ChunkSyncingManager;

public class Globals {
    public static PortalRenderManager portalRenderManager = new PortalRenderManager();
    
    public static ShaderManager shaderManager = new ShaderManager();
    
    //TODO unload it at appropriate time
    public static ClientWorldLoader clientWorldLoader = new ClientWorldLoader();
    
    public static ChunkSyncingManager chunkSyncingManager;
    
    public static MyGameRenderer myGameRenderer = new MyGameRenderer();
    
    public static ServerTeleportationManager serverTeleportationManager = new ServerTeleportationManager();
    
    public static ClientTeleportationManager clientTeleportationManager = new ClientTeleportationManager();
}
