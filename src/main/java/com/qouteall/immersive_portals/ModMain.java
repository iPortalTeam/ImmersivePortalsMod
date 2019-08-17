package com.qouteall.immersive_portals;

import com.qouteall.immersive_portals.chunk_loading.ChunkDataSyncManager;
import com.qouteall.immersive_portals.chunk_loading.ChunkTracker;
import com.qouteall.immersive_portals.my_util.Helper;
import com.qouteall.immersive_portals.my_util.MyTaskList;
import com.qouteall.immersive_portals.my_util.Signal;
import com.qouteall.immersive_portals.portal.MonitoringNetherPortal;
import com.qouteall.immersive_portals.portal.MyNetherPortalBlock;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.teleportation.ServerTeleportationManager;
import net.fabricmc.api.ModInitializer;

public class ModMain implements ModInitializer {
    //after world ticking
    public static final Signal postClientTickSignal = new Signal();
    public static final Signal postServerTickSignal = new Signal();
    public static final Signal preRenderSignal = new Signal();
    public static final MyTaskList clientTaskList = new MyTaskList();
    public static final MyTaskList serverTaskList = new MyTaskList();
    public static final MyTaskList preRenderTaskList = new MyTaskList();
    
    @Override
    public void onInitialize() {
        Helper.log("initializing common");
        
        Portal.init();
        MonitoringNetherPortal.init();
    
        MyNetherPortalBlock.init();
    
        MyNetwork.init();
        
        postClientTickSignal.connect(clientTaskList::processTasks);
        postServerTickSignal.connect(serverTaskList::processTasks);
        preRenderSignal.connect(preRenderTaskList::processTasks);
    
        SGlobal.serverTeleportationManager = new ServerTeleportationManager();
        SGlobal.chunkTracker = new ChunkTracker();
        SGlobal.chunkDataSyncManager = new ChunkDataSyncManager();
    }
    
}
