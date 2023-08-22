package qouteall.imm_ptl.core.platform_specific;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import qouteall.imm_ptl.core.portal.BreakableMirror;
import qouteall.imm_ptl.core.portal.EndPortalEntity;
import qouteall.imm_ptl.core.portal.LoadingIndicatorEntity;
import qouteall.imm_ptl.core.portal.Mirror;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalPlaceholderBlock;
import qouteall.imm_ptl.core.portal.global_portals.GlobalTrackedPortal;
import qouteall.imm_ptl.core.portal.global_portals.VerticalConnectingPortal;
import qouteall.imm_ptl.core.portal.global_portals.WorldWrappingPortal;
import qouteall.imm_ptl.core.portal.nether_portal.GeneralBreakablePortal;
import qouteall.imm_ptl.core.portal.nether_portal.NetherPortalEntity;

import java.util.function.BiConsumer;

public class IPRegistry {
    
    public static void registerBlocks(BiConsumer<ResourceLocation, PortalPlaceholderBlock> regFunc) {
        regFunc.accept(
            new ResourceLocation("immersive_portals", "nether_portal_block"), PortalPlaceholderBlock.instance
        );
    }
    
    public static void registerEntityTypes(BiConsumer<ResourceLocation, EntityType<?>> regFunc) {
    
        regFunc.accept(
            new ResourceLocation("immersive_portals", "portal"),
            Portal.entityType
        );
        
        regFunc.accept(
            new ResourceLocation("immersive_portals", "nether_portal_new"),
            NetherPortalEntity.entityType
        );
        
        regFunc.accept(
            new ResourceLocation("immersive_portals", "end_portal"),
            EndPortalEntity.entityType
        );
        
        regFunc.accept(
            new ResourceLocation("immersive_portals", "mirror"),
            Mirror.entityType
        );
        
        regFunc.accept(
            new ResourceLocation("immersive_portals", "breakable_mirror"),
            BreakableMirror.entityType
        );
        
        regFunc.accept(
            new ResourceLocation("immersive_portals", "global_tracked_portal"),
            GlobalTrackedPortal.entityType
        );
        
        regFunc.accept(
            new ResourceLocation("immersive_portals", "border_portal"),
            WorldWrappingPortal.entityType
        );
        
        regFunc.accept(
            new ResourceLocation("immersive_portals", "end_floor_portal"),
            VerticalConnectingPortal.entityType
        );
        
        regFunc.accept(
            new ResourceLocation("immersive_portals", "general_breakable_portal"),
            GeneralBreakablePortal.entityType
        );
        
        regFunc.accept(
            new ResourceLocation("immersive_portals", "loading_indicator"),
            LoadingIndicatorEntity.entityType
        );
    }
}
