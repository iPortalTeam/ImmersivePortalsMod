package com.qouteall.immersive_portals;

import com.qouteall.immersive_portals.my_util.Helper;
import com.qouteall.immersive_portals.optifine_compatibility.OFGlobal;
import com.qouteall.immersive_portals.optifine_compatibility.OFHelper;
import com.qouteall.immersive_portals.portal.*;
import com.qouteall.immersive_portals.render.MyGameRenderer;
import com.qouteall.immersive_portals.render.PortalRenderer;
import com.qouteall.immersive_portals.render.RendererUsingFrameBuffer;
import com.qouteall.immersive_portals.render.RendererUsingStencil;
import com.qouteall.immersive_portals.teleportation.ClientTeleportationManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.render.EntityRendererRegistry;
import net.fabricmc.loader.FabricLoader;
import net.minecraft.client.MinecraftClient;

public class ModMainClient implements ClientModInitializer {
    
    public static void initPortalRenderers() {
        EntityRendererRegistry.INSTANCE.register(
            Portal.class,
            (entityRenderDispatcher, context) -> new PortalDummyRenderer(entityRenderDispatcher)
        );
        EntityRendererRegistry.INSTANCE.register(
            NetherPortalEntity.class,
            (entityRenderDispatcher, context) -> new PortalDummyRenderer(entityRenderDispatcher)
        );
        EntityRendererRegistry.INSTANCE.register(
            EndPortalEntity.class,
            (entityRenderDispatcher, context) -> new PortalDummyRenderer(entityRenderDispatcher)
        );
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
                switchRenderer(OFGlobal.rendererDeferred);
            }
        }
        else {
            switchRenderer(CGlobal.rendererUsingStencil);
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
        LoadingIndicatorEntity.initClient();
    
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
    
        if (CGlobal.isOptifinePresent) {
            CGlobal.renderPortalBeforeTranslucentBlocks = false;
        
            OFHelper.init();

//            if (Config.isSmoothWorld()) {
//                //TODO change smooth world to false
//                Helper.err("Smooth world will cause entity in other dimension to vanish");
//            }
        }
    }
}
