package qouteall.imm_ptl.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import qouteall.imm_ptl.core.chunk_loading.ChunkDataSyncManager;
import qouteall.imm_ptl.core.teleportation.ServerTeleportationManager;
import qouteall.q_misc_util.my_util.MyTaskList;
import qouteall.q_misc_util.my_util.Signal;

public class IPGlobal {
    
    public static final Signal postClientTickSignal = new Signal();
    public static final Signal postServerTickSignal = new Signal();
    public static final Signal preGameRenderSignal = new Signal();
    
    // executed after ticking. will be cleared when client encounter loading screen
    public static final MyTaskList clientTaskList = new MyTaskList();
    
    // executed after ticking. will be cleared when server closes
    public static final MyTaskList serverTaskList = new MyTaskList();
    
    // won't be cleared
    public static final MyTaskList preGameRenderTaskList = new MyTaskList();
    public static final MyTaskList preTotalRenderTaskList = new MyTaskList();
    public static final Signal clientCleanupSignal = new Signal();
    public static final Signal serverCleanupSignal = new Signal();
    
    public static ChunkDataSyncManager chunkDataSyncManager;
    
    public static ServerTeleportationManager serverTeleportationManager;
    
    public static Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    public static int maxPortalLayer = 5;
    
    public static int indirectLoadingRadiusCap = 8;
    
    public static boolean lagAttackProof = true;
    
    public static RenderMode renderMode = RenderMode.normal;
    
    public static boolean doCheckGlError = true;
    
    public static boolean renderYourselfInPortal = true;
    
    public static boolean activeLoading = true;
    
    public static int netherPortalFindingRadius = 128;
    
    public static boolean teleportationDebugEnabled = false;
    
    public static boolean correctCrossPortalEntityRendering = true;
    
    public static boolean disableTeleportation = false;
    
    public static boolean looseVisibleChunkIteration = true;
    
    public static boolean looseMovementCheck = false;
    
    public static boolean pureMirror = false;
    
    public static int portalRenderLimit = 200;
    
    public static boolean cacheGlBuffer = true;
    
    public static boolean enableAlternateDimensions = true;
    
    public static boolean reducedPortalRendering = false;
    
    public static boolean useSecondaryEntityVertexConsumer = true;
    
    public static boolean cullSectionsBehind = true;
    
    public static boolean offsetOcclusionQuery = true;
    
    public static boolean cloudOptimization = true;
    
    public static boolean crossPortalCollision = true;
    
    public static int chunkUnloadDelayTicks = 10 * 20;
    
    public static boolean enablePortalRenderingMerge = true;
    
    public static boolean forceMergePortalRendering = false;
    
    public static boolean netherPortalOverlay = false;
    
    public static boolean debugDisableFog = false;
    
    public static int scaleLimit = 30;
    
    public static boolean easeCreativePermission = true;
    public static boolean easeCommandStickPermission = true;
    
    public static boolean enableDepthClampForPortalRendering = false;
    
    public static boolean enableServerCollision = true;
    
    public static boolean enableSharedBlockMeshBuffers = true;
    
    public static boolean enableDatapackPortalGen = true;
    
    public static boolean enableCrossPortalView = true;
    
    public static boolean enableNetherPortalNoise = false;
    
    public static boolean enableClippingMechanism = true;
    
    public static boolean viewBobbingCameraCorrection = false;
    
    public static boolean enableWarning = true;
    
    public static boolean lightVanillaNetherPortalWhenCrouching = false;
    
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
