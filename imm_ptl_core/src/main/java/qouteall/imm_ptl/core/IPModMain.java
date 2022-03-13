package qouteall.imm_ptl.core;

import qouteall.imm_ptl.core.chunk_loading.ChunkDataSyncManager;
import qouteall.imm_ptl.core.chunk_loading.EntitySync;
import qouteall.imm_ptl.core.chunk_loading.MyLoadingTicket;
import qouteall.imm_ptl.core.chunk_loading.NewChunkTrackingGraph;
import qouteall.imm_ptl.core.chunk_loading.ServerPerformanceMonitor;
import qouteall.imm_ptl.core.chunk_loading.WorldInfoSender;
import qouteall.imm_ptl.core.miscellaneous.GcMonitor;
import qouteall.imm_ptl.core.platform_specific.IPNetworking;
import qouteall.imm_ptl.core.portal.PortalExtension;
import qouteall.imm_ptl.core.portal.global_portals.GlobalPortalStorage;
import qouteall.imm_ptl.core.render.VisibleSectionDiscovery;
import qouteall.imm_ptl.core.teleportation.CollisionHelper;
import qouteall.imm_ptl.core.teleportation.ServerTeleportationManager;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.LifecycleHack;

public class IPModMain {
    
    public static void init() {
        Helper.log("Immersive Portals Mod Initializing");
        
        IPNetworking.init();
        
        IPGlobal.postClientTickSignal.connect(IPGlobal.clientTaskList::processTasks);
        IPGlobal.postServerTickSignal.connect(IPGlobal.serverTaskList::processTasks);
        IPGlobal.preGameRenderSignal.connect(IPGlobal.preGameRenderTaskList::processTasks);
        
        IPGlobal.clientCleanupSignal.connect(() -> {
            if (ClientWorldLoader.getIsInitialized()) {
                IPGlobal.clientTaskList.forceClearTasks();
            }
        });
        IPGlobal.serverCleanupSignal.connect(IPGlobal.serverTaskList::forceClearTasks);
        
        IPGlobal.serverTeleportationManager = new ServerTeleportationManager();
        IPGlobal.chunkDataSyncManager = new ChunkDataSyncManager();
        
        NewChunkTrackingGraph.init();
        
        WorldInfoSender.init();
        
        GlobalPortalStorage.init();
        
        EntitySync.init();
        
        CollisionHelper.init();
        
        PortalExtension.init();
        
        GcMonitor.initCommon();
        
        ServerPerformanceMonitor.init();
    
        MyLoadingTicket.init();
        
        LifecycleHack.markNamespaceStable("immersive_portals");
        LifecycleHack.markNamespaceStable("imm_ptl");
    }
    
}
