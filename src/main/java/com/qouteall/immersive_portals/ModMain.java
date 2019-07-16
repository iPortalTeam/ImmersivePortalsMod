package com.qouteall.immersive_portals;

import com.qouteall.immersive_portals.chunk_loading.ChunkDataSyncManager;
import com.qouteall.immersive_portals.chunk_loading.ChunkTracker;
import com.qouteall.immersive_portals.client_world_management.ClientWorldLoader;
import com.qouteall.immersive_portals.my_util.MyTaskList;
import com.qouteall.immersive_portals.my_util.Signal;
import com.qouteall.immersive_portals.nether_portal_managing.BlockMyNetherPortal;
import com.qouteall.immersive_portals.nether_portal_managing.MonitoringNetherPortal;
import com.qouteall.immersive_portals.portal_entity.Portal;
import com.qouteall.immersive_portals.render.MyGameRenderer;
import com.qouteall.immersive_portals.render.PortalRenderManager;
import com.qouteall.immersive_portals.render.ShaderManager;
import com.qouteall.immersive_portals.teleportation.ClientTeleportationManager;
import com.qouteall.immersive_portals.teleportation.ServerTeleportationManager;
import net.fabricmc.api.ModInitializer;
import net.minecraft.client.MinecraftClient;

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
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.
        Portal.init();
        MonitoringNetherPortal.init();
    
        BlockMyNetherPortal.init();
        
        MyNetwork.init();
    
        postClientTickSignal.connect(clientTaskList::processTasks);
        postServerTickSignal.connect(serverTaskList::processTasks);
        preRenderSignal.connect(preRenderTaskList::processTasks);
    
        //TODO make it compatible with dedicated server
        MinecraftClient.getInstance().execute(() -> {
            Globals.portalRenderManager = new PortalRenderManager();
            Globals.shaderManager = new ShaderManager();
            Globals.clientWorldLoader = new ClientWorldLoader();
            Globals.chunkTracker = new ChunkTracker();
            Globals.chunkDataSyncManager = new ChunkDataSyncManager();
            Globals.myGameRenderer = new MyGameRenderer();
            Globals.serverTeleportationManager = new ServerTeleportationManager();
            Globals.clientTeleportationManager = new ClientTeleportationManager();
        });
    }
}
