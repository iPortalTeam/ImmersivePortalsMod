package com.qouteall.immersive_portals;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.qouteall.immersive_portals.chunk_loading.ChunkDataSyncManager;
import com.qouteall.immersive_portals.teleportation.ServerTeleportationManager;

public class Global {
    
    public static ChunkDataSyncManager chunkDataSyncManager;
    
    public static ServerTeleportationManager serverTeleportationManager;
    
    public static Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    public static int maxPortalLayer = 5;
    
    public static int indirectLoadingRadiusCap = 8;
    
    public static boolean lagAttackProof = true;
    
    public static RenderMode renderMode = RenderMode.normal;
    
    public static boolean doCheckGlError = false;
    
    public static boolean renderYourselfInPortal = true;
    
    public static boolean activeLoading = true;
    
    public static int netherPortalFindingRadius = 128;
    
    public static boolean teleportationDebugEnabled = false;
    
    public static boolean correctCrossPortalEntityRendering = true;
    
    public static boolean multiThreadedNetherPortalSearching = true;
    
    public static boolean edgelessSky = false;
    
    public static boolean disableTeleportation = false;
    
    public static boolean looseVisibleChunkIteration = true;
    
    public static boolean looseMovementCheck = false;
    
    public static boolean pureMirror = false;
    
    public static int portalRenderLimit = 200;
    
    public static boolean cacheGlBuffer = true;
    
    public static boolean enableAlternateDimensions = true;
    
    public static boolean serverSmoothLoading = true;
    
    public static boolean reducedPortalRendering = false;
    
    public static boolean useSecondaryEntityVertexConsumer = true;
    
    public static boolean cullSectionsBehind = true;
    
    public static boolean offsetOcclusionQuery = true;
    
    public static boolean cloudOptimization = true;
    
    public static boolean crossPortalCollision = true;
    
    public static int chunkUnloadDelayTicks = 15 * 20;
    
    public static boolean enablePortalRenderingMerge = true;
    
    public static boolean forceMergePortalRendering = false;
    
    public static boolean netherPortalOverlay = false;
    
    public static boolean lightLogging = false;
    
    public static boolean debugDisableFog = false;
    
    public static int scaleLimit = 30;
    
    public static enum RenderMode {
        normal,
        compatibility,
        debug,
        none
    }
    
    // this should not be in core but the config is in core
    public static enum NetherPortalMode {
        normal,
        vanilla,
        adaptive,
        disabled
    }
    
    public static enum EndPortalMode {
        normal,
        toObsidianPlatform,
        scaledView,
        vanilla
    }
    
    public static NetherPortalMode netherPortalMode = NetherPortalMode.normal;
    
    public static EndPortalMode endPortalMode = EndPortalMode.normal;
}
