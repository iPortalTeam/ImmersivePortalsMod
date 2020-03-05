package com.qouteall.immersive_portals;

import com.qouteall.hiding_in_the_bushes.MyConfig;
import com.qouteall.hiding_in_the_bushes.MyNetworkClient;
import com.qouteall.immersive_portals.far_scenery.FarSceneryRenderer;
import com.qouteall.immersive_portals.optifine_compatibility.OFGlobal;
import com.qouteall.immersive_portals.portal.BreakableMirror;
import com.qouteall.immersive_portals.portal.EndPortalEntity;
import com.qouteall.immersive_portals.portal.LoadingIndicatorEntity;
import com.qouteall.immersive_portals.portal.Mirror;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.portal.global_portals.BorderPortal;
import com.qouteall.immersive_portals.portal.global_portals.GlobalTrackedPortal;
import com.qouteall.immersive_portals.portal.global_portals.VerticalConnectingPortal;
import com.qouteall.immersive_portals.portal.nether_portal.NewNetherPortalEntity;
import com.qouteall.immersive_portals.render.LoadingIndicatorRenderer;
import com.qouteall.immersive_portals.render.MyGameRenderer;
import com.qouteall.immersive_portals.render.PortalEntityRenderer;
import com.qouteall.immersive_portals.render.PortalRenderer;
import com.qouteall.immersive_portals.render.RendererUsingFrameBuffer;
import com.qouteall.immersive_portals.render.RendererUsingStencil;
import com.qouteall.immersive_portals.teleportation.ClientTeleportationManager;
import net.fabricmc.fabric.api.client.rendereregistry.v1.EntityRendererRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityType;
import org.apache.commons.lang3.Validate;

import java.util.Arrays;

public class ModMainClient {
    public static void initPortalRenderers() {
        
        Arrays.stream(new EntityType<?>[]{
            Portal.entityType,
            NewNetherPortalEntity.entityType,
            EndPortalEntity.entityType,
            Mirror.entityType,
            BreakableMirror.entityType,
            GlobalTrackedPortal.entityType,
            BorderPortal.entityType,
            VerticalConnectingPortal.entityType
        }).peek(
            Validate::notNull
        ).forEach(
            entityType -> EntityRendererRegistry.INSTANCE.register(
                entityType,
                (entityRenderDispatcher, context) -> new PortalEntityRenderer(entityRenderDispatcher)
            )
        );
    
        EntityRendererRegistry.INSTANCE.register(
            LoadingIndicatorEntity.entityType,
            (entityRenderDispatcher, context) -> new LoadingIndicatorRenderer(entityRenderDispatcher)
        );
    
    }
    
    public static void switchToCorrectRenderer() {
        if (CGlobal.renderer.isRendering()) {
            //do not switch when rendering
            return;
        }
        if (OFInterface.isShaders.getAsBoolean()) {
            switch (Global.renderMode) {
                case normal:
                    switchRenderer(OFGlobal.rendererMixed);
                    break;
                case compatibility:
                    switchRenderer(OFGlobal.rendererDeferred);
                    break;
                case debug:
                    switchRenderer(OFGlobal.rendererDebugWithShader);
                    break;
                case none:
                    switchRenderer(CGlobal.rendererDummy);
                    break;
            }
        }
        else {
            switch (Global.renderMode) {
                case normal:
                    switchRenderer(CGlobal.rendererUsingStencil);
                    break;
                case compatibility:
                    switchRenderer(CGlobal.rendererUsingFrameBuffer);
                    break;
                case debug:
                    //TODO add debug renderer for non shader mode
                    break;
                case none:
                    switchRenderer(CGlobal.rendererDummy);
                    break;
            }
        }
    }
    
    private static void switchRenderer(PortalRenderer renderer) {
        if (CGlobal.renderer != renderer) {
            Helper.log("switched to renderer " + renderer.getClass());
            CGlobal.renderer = renderer;
        }
    }
    
    public static void init() {
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
        
        FarSceneryRenderer.init();
        
        MyConfig.onConfigChanged(MyConfig.readConfigFromFile());
        
        DubiousLightUpdate.init();
    }
}
