package com.qouteall.immersive_portals;

import com.google.gson.Gson;
import com.qouteall.immersive_portals.chunk_loading.ChunkDataSyncManager;
import com.qouteall.immersive_portals.teleportation.ServerTeleportationManager;

public class Global {
    
    public static ChunkDataSyncManager chunkDataSyncManager;
    
    public static ServerTeleportationManager serverTeleportationManager;
    
    public static Gson gson = new Gson();
    
    public static int maxPortalLayer = 5;
    
    public static RenderMode renderMode = RenderMode.normal;
    
    public static boolean doCheckGlError = false;
    
    public static boolean longerReachInCreative = true;
    
    public static boolean renderYourselfInPortal = true;
    
    public static boolean activeLoading = true;
    
    public static int netherPortalFindingRadius = 128;
    
    public static boolean teleportationDebugEnabled = false;
    
    public static boolean correctCrossPortalEntityRendering = true;
    
    public static boolean loadFewerChunks = false;
    
    public static boolean multiThreadedNetherPortalSearching = true;
    
    public static boolean edgelessSky = false;
    
    public static boolean disableTeleportation = false;
    
    public static boolean reversibleNetherPortalLinking = false;
    
    public static boolean looseVisibleChunkIteration = true;
    
    public static boolean blameOpenJdk = true;

    public static boolean mirrorInteractableThroughPortal = true;

    public static enum RenderMode {
        normal,
        compatibility,
        debug,
        none
    }
}
