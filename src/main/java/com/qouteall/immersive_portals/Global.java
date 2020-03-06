package com.qouteall.immersive_portals;

import com.google.gson.Gson;
import com.qouteall.immersive_portals.chunk_loading.ChunkDataSyncManager;
import com.qouteall.immersive_portals.teleportation.ServerTeleportationManager;

public class Global {
    
    public static ChunkDataSyncManager chunkDataSyncManager;
    
    public static ServerTeleportationManager serverTeleportationManager;
    
    public static boolean isChunkLoadingMultiThreaded = true;
    
    public static Gson gson = new Gson();
    
    public static int maxPortalLayer = 5;
    
    public static RenderMode renderMode = RenderMode.normal;
    
    public static boolean doCheckGlError = false;
    
    public static boolean longerReachInCreative = true;
    
    public static boolean renderYourselfInPortal = true;
    
    public static enum RenderMode {
        normal,
        compatibility,
        debug,
        none
    }
}
