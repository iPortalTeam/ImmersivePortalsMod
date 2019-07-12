package com.qouteall.immersive_portals.portal_entity;

import com.qouteall.immersive_portals.my_util.Helper;
import com.qouteall.immersive_portals.nether_portal_managing.NetherPortalLifeCycleManager;
import com.qouteall.immersive_portals.nether_portal_managing.NetherPortalMatcher;
import com.qouteall.immersive_portals.nether_portal_managing.ObsidianFrame;
import net.fabricmc.fabric.api.client.render.EntityRendererRegistry;
import net.fabricmc.fabric.api.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityCategory;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

import java.util.UUID;

public class MonitoringNetherPortal extends Portal {
    public static EntityType<MonitoringNetherPortal> entityType;
    
    public DimensionType dimension1;
    public ObsidianFrame obsidianFrame1;
    public DimensionType dimension2;
    public ObsidianFrame obsidianFrame2;
    public UUID otherPortalId1;
    public UUID otherPortalId2;
    public UUID otherPortalId3;
    
    public static void init() {
        entityType = Registry.register(
            Registry.ENTITY_TYPE,
            new Identifier("immersive_portals", "monitoring_nether_portal"),
            FabricEntityTypeBuilder.create(
                EntityCategory.MISC,
                (EntityType<MonitoringNetherPortal> type, World world1) ->
                    new MonitoringNetherPortal(type, world1)
            ).size(
                new EntityDimensions(1, 1, true)
            ).build()
        );
        
        EntityRendererRegistry.INSTANCE.register(
            MonitoringNetherPortal.class,
            (entityRenderDispatcher, context) -> new PortalDummyRenderer(entityRenderDispatcher)
        );
    }
    
    public MonitoringNetherPortal(
        EntityType type,
        World world
    ) {
        super(type, world);
    }
    
    public MonitoringNetherPortal(
        World world
    ) {
        super(entityType, world);
    }
    
    //if the region is not loaded, it will return true
    public boolean checkNetherPortalIfLoaded() {
        assert Helper.getServer() != null;
        
        return checkObsidianFrameIfLoaded(dimension1, obsidianFrame1) &&
            checkObsidianFrameIfLoaded(dimension2, obsidianFrame2);
    }
    
    //if the region is not loaded, it will return true
    private boolean checkObsidianFrameIfLoaded(
        DimensionType dimension,
        ObsidianFrame obsidianFrame
    ) {
        ServerWorld world = Helper.getServer().getWorld(dimension);
        
        if (world == null) {
            return true;
        }
        
        if (!world.isBlockLoaded(obsidianFrame.boxWithoutObsidian.l)) {
            return true;
        }
        
        if (!NetherPortalMatcher.checkObsidianFrame(
            world,
            obsidianFrame.normalAxis,
            obsidianFrame.boxWithoutObsidian
        )) {
            return false;
        }
        
        return NetherPortalLifeCycleManager.checkInnerPortalBlocks(world, obsidianFrame);
    }
}
