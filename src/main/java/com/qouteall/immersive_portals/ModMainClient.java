package com.qouteall.immersive_portals;

import com.qouteall.immersive_portals.optifine_compatibility.OFGlobal;
import com.qouteall.immersive_portals.optifine_compatibility.OFHelper;
import com.qouteall.immersive_portals.portal.*;
import com.qouteall.immersive_portals.portal.global_portals.BorderPortal;
import com.qouteall.immersive_portals.portal.global_portals.EndFloorPortal;
import com.qouteall.immersive_portals.portal.global_portals.GlobalTrackedPortal;
import com.qouteall.immersive_portals.portal.nether_portal.NetherPortalEntity;
import com.qouteall.immersive_portals.portal.nether_portal.NewNetherPortalEntity;
import com.qouteall.immersive_portals.render.*;
import com.qouteall.immersive_portals.teleportation.ClientTeleportationManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.render.EntityRendererRegistry;
import net.fabricmc.loader.FabricLoader;
import net.minecraft.client.MinecraftClient;

import java.util.Arrays;

public class ModMainClient implements ClientModInitializer {
    
    public static void initPortalRenderers() {
        Arrays.stream(new Class[]{
            Portal.class,
            NetherPortalEntity.class,
            NewNetherPortalEntity.class,
            EndPortalEntity.class,
            Mirror.class,
            BreakableMirror.class,
            GlobalTrackedPortal.class,
            BorderPortal.class,
            EndFloorPortal.class
    
        }).forEach(
            portalClass -> EntityRendererRegistry.INSTANCE.register(
                portalClass,
                (entityRenderDispatcher, context) -> new PortalEntityRenderer(entityRenderDispatcher)
            )
        );
    
        EntityRendererRegistry.INSTANCE.register(
            LoadingIndicatorEntity.class,
            (entityRenderDispatcher, context) -> new LoadingIndicatorRenderer(entityRenderDispatcher)
        );
//
//        EntityRendererRegistry.INSTANCE.register(
//            Portal.class,
//            (entityRenderDispatcher, context) -> new PortalEntityRenderer(entityRenderDispatcher)
//        );
//        EntityRendererRegistry.INSTANCE.register(
//            NetherPortalEntity.class,
//            (entityRenderDispatcher, context) -> new PortalEntityRenderer(entityRenderDispatcher)
//        );
//        EntityRendererRegistry.INSTANCE.register(
//            EndPortalEntity.class,
//            (entityRenderDispatcher, context) -> new PortalEntityRenderer(entityRenderDispatcher)
//        );
//        EntityRendererRegistry.INSTANCE.register(
//            Mirror.class,
//            (entityRenderDispatcher, context) -> new PortalEntityRenderer(entityRenderDispatcher)
//        );
//        EntityRendererRegistry.INSTANCE.register(
//            BreakableMirror.class,
//            (entityRenderDispatcher, context) -> new PortalEntityRenderer(entityRenderDispatcher)
//        );
//        EntityRendererRegistry.INSTANCE.register(
//            GlobalTrackedPortal.class,
//            (entityRenderDispatcher, context) -> new PortalEntityRenderer(entityRenderDispatcher)
//        );
    }
    
    public static void switchToCorrectRenderer() {
        if (CGlobal.renderer.isRendering()) {
            //do not switch when rendering
            return;
        }
        if (OFHelper.getIsUsingShader()) {
            if (CGlobal.isRenderDebugMode) {
                switchRenderer(OFGlobal.rendererDebugWithShader);
            }
            else {
                switchRenderer(OFGlobal.rendererMixed);
            }
        }
        else {
            if (CGlobal.useCompatibilityRenderer || SatinCompatibility.isSatinShaderEnabled()) {
                switchRenderer(CGlobal.rendererUsingFrameBuffer);
            }
            else {
                switchRenderer(CGlobal.rendererUsingStencil);
            }
        }
    }
    
    private static void switchRenderer(PortalRenderer renderer) {
        if (CGlobal.renderer != renderer) {
            Helper.log("switched to renderer " + renderer.getClass());
            CGlobal.renderer = renderer;
        }
    }
    
    @Override
    public void onInitializeClient() {
        Helper.log("initializing client");
    
        initPortalRenderers();
    
    
        MyNetworkClient.init();
    
        MinecraftClient.getInstance().execute(() -> {
            CGlobal.rendererUsingStencil = new RendererUsingStencil();
            CGlobal.rendererUsingFrameBuffer = new RendererUsingFrameBuffer();
        
            CGlobal.renderer = CGlobal.rendererUsingStencil;
            CGlobal.clientWorldLoader = new ClientWorldLoader();
            CGlobal.myGameRenderer = new MyGameRenderer();
            CGlobal.clientTeleportationManager = new ClientTeleportationManager();
        });
    
        CGlobal.isOptifinePresent = FabricLoader.INSTANCE.isModLoaded("optifabric");
    
        Helper.log(CGlobal.isOptifinePresent ? "Optifine is present" : "Optifine is not present");
    
        SatinCompatibility.init();
    }
}
