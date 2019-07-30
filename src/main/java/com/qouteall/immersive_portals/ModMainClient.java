package com.qouteall.immersive_portals;

import com.qouteall.immersive_portals.my_util.Helper;
import com.qouteall.immersive_portals.portal.LoadingIndicatorEntity;
import com.qouteall.immersive_portals.portal.MonitoringNetherPortal;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.portal.PortalDummyRenderer;
import com.qouteall.immersive_portals.render.MyGameRenderer;
import com.qouteall.immersive_portals.render.PortalRenderManager;
import com.qouteall.immersive_portals.teleportation.ClientTeleportationManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.render.EntityRendererRegistry;
import net.minecraft.client.MinecraftClient;

public class ModMainClient implements ClientModInitializer {
    public static void portal_initClient() {
        EntityRendererRegistry.INSTANCE.register(
            Portal.class,
            (entityRenderDispatcher, context) -> new PortalDummyRenderer(entityRenderDispatcher)
        );
    }
    
    public static void nether_initClient() {
        EntityRendererRegistry.INSTANCE.register(
            MonitoringNetherPortal.class,
            (entityRenderDispatcher, context) -> new PortalDummyRenderer(entityRenderDispatcher)
        );
    }
    
    @Override
    public void onInitializeClient() {
        Helper.log("initializing client");
    
        portal_initClient();
        nether_initClient();
        LoadingIndicatorEntity.initClient();
    
        MyNetworkClient.init();
    
        MinecraftClient.getInstance().execute(() -> {
            Globals.portalRenderManager = new PortalRenderManager();
            Globals.clientWorldLoader = new ClientWorldLoader();
            Globals.myGameRenderer = new MyGameRenderer();
            Globals.clientTeleportationManager = new ClientTeleportationManager();
        });
    }
}
