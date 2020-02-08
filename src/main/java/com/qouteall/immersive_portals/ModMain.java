package com.qouteall.immersive_portals;

import com.qouteall.immersive_portals.alternate_dimension.AlternateDimensionInit;
import com.qouteall.immersive_portals.chunk_loading.ChunkDataSyncManager;
import com.qouteall.immersive_portals.chunk_loading.NewChunkTrackingGraph;
import com.qouteall.immersive_portals.chunk_loading.ServerPerformanceAdjust;
import com.qouteall.immersive_portals.chunk_loading.WorldInfoSender;
import com.qouteall.immersive_portals.compat.RequiemCompat;
import com.qouteall.immersive_portals.my_util.MyTaskList;
import com.qouteall.immersive_portals.my_util.Signal;
import com.qouteall.immersive_portals.portal.*;
import com.qouteall.immersive_portals.portal.global_portals.BorderPortal;
import com.qouteall.immersive_portals.portal.global_portals.GlobalTrackedPortal;
import com.qouteall.immersive_portals.portal.global_portals.VerticalConnectingPortal;
import com.qouteall.immersive_portals.portal.nether_portal.NetherPortalEntity;
import com.qouteall.immersive_portals.portal.nether_portal.NewNetherPortalEntity;
import com.qouteall.immersive_portals.teleportation.ServerTeleportationManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.dimension.v1.FabricDimensionType;

public class ModMain implements ModInitializer {
    public static final Signal postClientTickSignal = new Signal();
    public static final Signal postServerTickSignal = new Signal();
    public static final Signal preRenderSignal = new Signal();
    public static final MyTaskList clientTaskList = new MyTaskList();
    public static final MyTaskList serverTaskList = new MyTaskList();
    public static final MyTaskList preRenderTaskList = new MyTaskList();
    
    public static FabricDimensionType alternate1;
    public static FabricDimensionType alternate2;
    public static FabricDimensionType alternate3;
    public static FabricDimensionType alternate4;
    public static FabricDimensionType alternate5;
    
    @Override
    public void onInitialize() {
        Helper.log("initializing common");
        
        Portal.init();
        NetherPortalEntity.init();
        NewNetherPortalEntity.init();
        EndPortalEntity.init();
        Mirror.init();
        BreakableMirror.init();
        GlobalTrackedPortal.init();
        BorderPortal.init();
        VerticalConnectingPortal.init();
        
        LoadingIndicatorEntity.init();
        
        PortalPlaceholderBlock.init();
        
        MyNetwork.init();
        
        postClientTickSignal.connect(clientTaskList::processTasks);
        postServerTickSignal.connect(serverTaskList::processTasks);
        preRenderSignal.connect(preRenderTaskList::processTasks);
        
        SGlobal.serverTeleportationManager = new ServerTeleportationManager();
        SGlobal.chunkDataSyncManager = new ChunkDataSyncManager();
        
        NewChunkTrackingGraph.init();
        
        WorldInfoSender.init();
        
        ServerPerformanceAdjust.init();
        
        AlternateDimensionInit.initMyDimensions();
        
        RequiemCompat.init();
    }
    
}
