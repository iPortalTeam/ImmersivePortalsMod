package com.qouteall.immersive_portals.portal;

import com.qouteall.immersive_portals.MyNetwork;
import com.qouteall.immersive_portals.my_util.Helper;
import com.qouteall.immersive_portals.my_util.IntegerAABBInclusive;
import net.fabricmc.fabric.api.client.render.EntityRendererRegistry;
import net.fabricmc.fabric.api.entity.FabricEntityTypeBuilder;
import net.minecraft.client.network.packet.CustomPayloadS2CPacket;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCategory;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Packet;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;

public class LoadingIndicatorEntity extends Entity {
    public static EntityType<LoadingIndicatorEntity> entityType;
    
    public static void init() {
        entityType = Registry.register(
            Registry.ENTITY_TYPE,
            new Identifier("immersive_portals", "loading_indicator"),
            FabricEntityTypeBuilder.create(
                EntityCategory.MISC,
                (EntityType.EntityFactory<LoadingIndicatorEntity>) LoadingIndicatorEntity::new
            ).size(
                new EntityDimensions(1, 1, true)
            ).build()
        );
        
        EntityRendererRegistry.INSTANCE.register(
            LoadingIndicatorEntity.class,
            (entityRenderDispatcher, context) -> new LoadingIndicatorRenderer(entityRenderDispatcher)
        );
    }
    
    public static void spawnLoadingIndicator(
        ServerWorld world,
        ObsidianFrame obsidianFrame
    ) {
        IntegerAABBInclusive box = obsidianFrame.boxWithoutObsidian;
        Vec3d center = new Vec3d(
            (double) (box.h.getX() + box.l.getX() + 1) / 2,
            (double) (box.h.getY() + box.l.getY() + 1) / 2 - 1,
            (double) (box.h.getZ() + box.l.getZ() + 1) / 2
        );
        CustomPayloadS2CPacket packet =
            MyNetwork.createSpawnLoadingIndicator(world.dimension.getType(), center);
        Helper.getEntitiesNearby(
            world, center, ServerPlayerEntity.class, 64
        ).forEach(
            player -> player.networkHandler.sendPacket(packet)
        );
    }
    
    public LoadingIndicatorEntity(World world) {
        this(entityType, world);
    }
    
    public LoadingIndicatorEntity(EntityType type, World world) {
        super(type, world);
    }
    
    @Override
    public Iterable<ItemStack> getArmorItems() {
        return null;
    }
    
    @Override
    public void tick() {
        super.tick();
        
        if (Helper.getEntitiesNearby(this, Portal.class, 3).findAny().isPresent()) {
            this.remove();
        }
    }
    
    @Override
    protected void initDataTracker() {
    
    }
    
    @Override
    protected void readCustomDataFromTag(CompoundTag var1) {
    
    }
    
    @Override
    protected void writeCustomDataToTag(CompoundTag var1) {
    
    }
    
    @Override
    public Packet<?> createSpawnPacket() {
        return null;
    }
}
