package com.qouteall.immersive_portals;

import com.qouteall.immersive_portals.chunk_loading.ChunkDataSyncManager;
import com.qouteall.immersive_portals.chunk_loading.ChunkTracker;
import com.qouteall.immersive_portals.client_world_management.ClientWorldLoader;
import com.qouteall.immersive_portals.my_util.MyTaskList;
import com.qouteall.immersive_portals.my_util.Signal;
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
    public static Signal clientTickSignal = new Signal();
    public static Signal serverTickSignal = new Signal();
    public static MyTaskList clientTaskList = new MyTaskList();
    public static MyTaskList serverTaskList = new MyTaskList();
    
    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.
        Portal.init();
        MonitoringNetherPortal.init();
        
        MyNetwork.init();
        
        clientTickSignal.connect(() -> clientTaskList.processTasks());
        serverTickSignal.connect(() -> serverTaskList.processTasks());
        
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
