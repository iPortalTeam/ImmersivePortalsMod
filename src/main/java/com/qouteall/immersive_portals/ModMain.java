package com.qouteall.immersive_portals;

import com.qouteall.immersive_portals.chunk_loading.ChunkDataSyncManager;
import com.qouteall.immersive_portals.chunk_loading.ChunkTracker;
import com.qouteall.immersive_portals.chunk_loading.WorldInfoSender;
import com.qouteall.immersive_portals.my_util.Helper;
import com.qouteall.immersive_portals.my_util.MyTaskList;
import com.qouteall.immersive_portals.my_util.Signal;
import com.qouteall.immersive_portals.portal.*;
import com.qouteall.immersive_portals.portal.global_portals.GlobalTrackedPortal;
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
        NetherPortalEntity.init();
        EndPortalEntity.init();
        Mirror.init();
        BreakableMirror.init();
        GlobalTrackedPortal.init();
    
        PortalPlaceholderBlock.init();
    
        MyNetwork.init();
        
        postClientTickSignal.connect(clientTaskList::processTasks);
        postServerTickSignal.connect(serverTaskList::processTasks);
        preRenderSignal.connect(preRenderTaskList::processTasks);
    
        SGlobal.serverTeleportationManager = new ServerTeleportationManager();
        SGlobal.chunkTracker = new ChunkTracker();
        SGlobal.chunkDataSyncManager = new ChunkDataSyncManager();
    
        WorldInfoSender.init();
    }
    
}
