package com.qouteall.immersive_portals;

import com.qouteall.hiding_in_the_bushes.MyNetwork;
import com.qouteall.immersive_portals.alternate_dimension.FormulaGenerator;
import com.qouteall.immersive_portals.chunk_loading.ChunkDataSyncManager;
import com.qouteall.immersive_portals.chunk_loading.NewChunkTrackingGraph;
import com.qouteall.immersive_portals.chunk_loading.WorldInfoSender;
import com.qouteall.immersive_portals.my_util.MyTaskList;
import com.qouteall.immersive_portals.my_util.Signal;
import com.qouteall.immersive_portals.portal.global_portals.GlobalPortalStorage;
import com.qouteall.immersive_portals.teleportation.ServerTeleportationManager;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.world.dimension.DimensionType;

public class ModMain {
    public static final Signal postClientTickSignal = new Signal();
    public static final Signal postServerTickSignal = new Signal();
    public static final Signal preRenderSignal = new Signal();
    public static final MyTaskList clientTaskList = new MyTaskList();
    public static final MyTaskList serverTaskList = new MyTaskList();
    public static final MyTaskList preRenderTaskList = new MyTaskList();
    
    public static Block portalHelperBlock;
    public static BlockItem portalHelperBlockItem;
    
    public static DimensionType alternate1;
    public static DimensionType alternate2;
    public static DimensionType alternate3;
    public static DimensionType alternate4;
    public static DimensionType alternate5;
    
    public static void init() {
        Helper.log("initializing common");
        
        MyNetwork.init();
        
        Helper.log("Network initialized");
        
        postClientTickSignal.connect(clientTaskList::processTasks);
        postServerTickSignal.connect(serverTaskList::processTasks);
        preRenderSignal.connect(preRenderTaskList::processTasks);
        
        Global.serverTeleportationManager = new ServerTeleportationManager();
        Global.chunkDataSyncManager = new ChunkDataSyncManager();
        
        Helper.log("Global objects initialized");
        
        NewChunkTrackingGraph.init();
        
        Helper.log("Chunk tracking graph initialized");
        
        WorldInfoSender.init();
        
        Helper.log("World info sender initialized");
        
        FormulaGenerator.init();
        
        GlobalPortalStorage.init();
        
        
    }
    
}
