package com.qouteall.immersive_portals.portal_entity;

import com.qouteall.immersive_portals.my_util.Helper;
import com.qouteall.immersive_portals.nether_portal_managing.BlockMyNetherPortal;
import com.qouteall.immersive_portals.nether_portal_managing.NetherPortalMatcher;
import com.qouteall.immersive_portals.nether_portal_managing.ObsidianFrame;
import net.fabricmc.fabric.api.client.render.EntityRendererRegistry;
import net.fabricmc.fabric.api.entity.FabricEntityTypeBuilder;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCategory;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.IWorld;
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
                (EntityType.EntityFactory<MonitoringNetherPortal>) MonitoringNetherPortal::new
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
    
    public void notifyCheckPortalIntegrity() {
        if (!world.isClient) {
            if (!checkNetherPortalIfLoaded()) {
                breakNetherPortal(this);
                this.removed = true;
                Entity entity2 = ((ServerWorld) world).getEntity(otherPortalId1);
                if (entity2 != null) {
                    entity2.removed = true;
                }
                Entity entity1 = Helper.getServer().getWorld(dimension2).getEntity(otherPortalId2);
                if (entity1 != null) {
                    entity1.removed = true;
                }
                Entity entity = Helper.getServer().getWorld(dimension2).getEntity(otherPortalId3);
                if (entity != null) {
                    entity.removed = true;
                }
            }
        }
    }
    
    //if the region is not loaded, it will return true
    private boolean checkNetherPortalIfLoaded() {
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
    
        return checkInnerPortalBlocks(world, obsidianFrame);
    }
    
    private static boolean checkInnerPortalBlocks(
        IWorld world,
        ObsidianFrame obsidianFrame
    ) {
        return obsidianFrame.boxWithoutObsidian.stream().allMatch(
            blockPos -> world.getBlockState(blockPos).getBlock()
                == BlockMyNetherPortal.instance
        );
    }
    
    private static void breakNetherPortal(
        MonitoringNetherPortal portalGuard
    ) {
        ServerWorld world1 = Helper.getServer().getWorld(portalGuard.dimension1);
        ServerWorld world2 = Helper.getServer().getWorld(portalGuard.dimension2);
        
        portalGuard.obsidianFrame1.boxWithoutObsidian.stream().forEach(
            blockPos -> world1.setBlockState(
                blockPos,
                Blocks.AIR.getDefaultState()
            )
        );
        
        portalGuard.obsidianFrame2.boxWithoutObsidian.stream().forEach(
            blockPos -> world2.setBlockState(
                blockPos,
                Blocks.AIR.getDefaultState()
            )
        );
        
        assert false;
    }
}
