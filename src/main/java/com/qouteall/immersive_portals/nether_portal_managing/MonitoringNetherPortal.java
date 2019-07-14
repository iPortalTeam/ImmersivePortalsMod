package com.qouteall.immersive_portals.nether_portal_managing;

import com.qouteall.immersive_portals.MyNetwork;
import com.qouteall.immersive_portals.my_util.Helper;
import com.qouteall.immersive_portals.portal_entity.Portal;
import com.qouteall.immersive_portals.portal_entity.PortalDummyRenderer;
import net.fabricmc.fabric.api.client.render.EntityRendererRegistry;
import net.fabricmc.fabric.api.entity.FabricEntityTypeBuilder;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCategory;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Packet;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

import java.util.UUID;

public class MonitoringNetherPortal extends Portal {
    public static EntityType<MonitoringNetherPortal> entityType;
    
    private boolean isNotified = false;
    
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
    
        BlockMyNetherPortal.portalBlockUpdateSignal.connect((world, pos) -> {
            Helper.getEntitiesNearby(
                world,
                new Vec3d(pos),
                entityType,
                20
            ).forEach(
                MonitoringNetherPortal::notifyToCheckIntegrity
            );
        });
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
    
    @Override
    public boolean isPortalValid() {
        return super.isPortalValid() &&
            dimension1 != null &&
            dimension2 != null &&
            obsidianFrame1 != null &&
            obsidianFrame2 != null;
    }
    
    public void notifyToCheckIntegrity() {
        isNotified = true;
    }
    
    @Override
    public void tick() {
        super.tick();
        
        if (!world.isClient) {
            if (isNotified) {
                isNotified = false;
                checkNetherPortalIfLoaded();
            }
        }
    }
    
    private void checkPortalIntegrity() {
        if (!isPortalValid()) {
            return;
        }
        
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
    
    @Override
    protected void readCustomDataFromTag(CompoundTag compoundTag) {
        super.readCustomDataFromTag(compoundTag);
        
        dimension1 = DimensionType.byRawId(compoundTag.getInt("dimension1"));
        dimension2 = DimensionType.byRawId(compoundTag.getInt("dimension2"));
        Tag frame1 = compoundTag.getTag("frame1");
        if (!(frame1 instanceof CompoundTag)) {
            Helper.err("bad nether portal data " + compoundTag);
            return;
        }
        obsidianFrame1 = ObsidianFrame.fromTag((CompoundTag) frame1);
        Tag frame2 = compoundTag.getTag("frame2");
        if (!(frame2 instanceof CompoundTag)) {
            Helper.err("bad nether portal data " + compoundTag);
            return;
        }
        obsidianFrame2 = ObsidianFrame.fromTag((CompoundTag) frame2);
    }
    
    @Override
    protected void writeCustomDataToTag(CompoundTag compoundTag) {
        super.writeCustomDataToTag(compoundTag);
        
        compoundTag.putInt("dimension1", dimension1.getRawId());
        compoundTag.putInt("dimension2", dimension2.getRawId());
        compoundTag.put("frame1", obsidianFrame1.toTag());
        compoundTag.put("frame2", obsidianFrame2.toTag());
    }
    
    @Override
    public Packet<?> createSpawnPacket() {
        return MyNetwork.createStcSpawnEntity(
            entityType,
            this
        );
    }
}
