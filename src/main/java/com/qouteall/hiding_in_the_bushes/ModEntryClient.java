package com.qouteall.hiding_in_the_bushes;

import com.qouteall.hiding_in_the_bushes.sodium_compatibility.SodiumInterfaceInitializer;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.ModMainClient;
import com.qouteall.immersive_portals.SodiumInterface;
import com.qouteall.immersive_portals.portal.BreakableMirror;
import com.qouteall.immersive_portals.portal.EndPortalEntity;
import com.qouteall.immersive_portals.portal.LoadingIndicatorEntity;
import com.qouteall.immersive_portals.portal.Mirror;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.portal.global_portals.GlobalTrackedPortal;
import com.qouteall.immersive_portals.portal.global_portals.VerticalConnectingPortal;
import com.qouteall.immersive_portals.portal.global_portals.WorldWrappingPortal;
import com.qouteall.immersive_portals.portal.nether_portal.GeneralBreakablePortal;
import com.qouteall.immersive_portals.portal.nether_portal.NetherPortalEntity;
import com.qouteall.immersive_portals.render.LoadingIndicatorRenderer;
import com.qouteall.immersive_portals.render.PortalEntityRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendereregistry.v1.EntityRendererRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.EntityType;
import org.apache.commons.lang3.Validate;

import java.util.Arrays;

public class ModEntryClient implements ClientModInitializer {
    
    public static void initPortalRenderers() {
        
        Arrays.stream(new EntityType<?>[]{
            Portal.entityType,
            NetherPortalEntity.entityType,
            EndPortalEntity.entityType,
            Mirror.entityType,
            BreakableMirror.entityType,
            GlobalTrackedPortal.entityType,
            WorldWrappingPortal.entityType,
            VerticalConnectingPortal.entityType,
            GeneralBreakablePortal.entityType
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
    
    @Override
    public void onInitializeClient() {
        ModMainClient.init();
        
        initPortalRenderers();
    
        SodiumInterface.isSodiumPresent =
            FabricLoader.getInstance().isModLoaded("sodium");
        if (SodiumInterface.isSodiumPresent) {
            Helper.log("Sodium is present");
            SodiumInterfaceInitializer.init();
        }
        else {
            Helper.log("Sodium is not present");
        }
    }
    
}
