package com.qouteall.immersive_portals.portal;

import net.fabricmc.fabric.api.entity.FabricEntityTypeBuilder;
import net.minecraft.block.pattern.BlockPattern;
import net.minecraft.entity.*;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

public class EndPortalEntity extends Portal {
    public static EntityType<EndPortalEntity> entityType;
    
    public static void init() {
        entityType = Registry.register(
            Registry.ENTITY_TYPE,
            new Identifier("immersive_portals", "end_portal"),
            FabricEntityTypeBuilder.create(
                EntityCategory.MISC,
                (EntityType.EntityFactory<EndPortalEntity>) EndPortalEntity::new
            ).size(
                new EntityDimensions(1, 1, true)
            ).setImmuneToFire().build()
        );
    }
    
    public EndPortalEntity(
        EntityType<?> entityType_1,
        World world_1
    ) {
        super(entityType_1, world_1);
    }
    
    public EndPortalEntity(World world) {
        this(entityType, world);
    }
    
    public static void onEndPortalComplete(ServerWorld world, BlockPattern.Result pattern) {
        Portal portal = new EndPortalEntity(world);
        
        Vec3d center = new Vec3d(pattern.getFrontTopLeft()).add(-1.5, 0.5, -1.5);
        portal.setPosition(center.x, center.y, center.z);
        
        portal.destination = new Vec3d(0, 120, 0);
        
        portal.dimensionTo = DimensionType.THE_END;
        
        portal.axisW = new Vec3d(0, 0, 1);
        portal.axisH = new Vec3d(1, 0, 0);
        
        portal.width = 3;
        portal.height = 3;
        
        portal.loadFewerChunks = false;
        
        world.spawnEntity(portal);
    }
    
    @Override
    public void onEntityTeleportedOnServer(Entity entity) {
        if (shouldAddSlowFalling(entity)) {
            LivingEntity livingEntity = (LivingEntity) entity;
            livingEntity.addPotionEffect(
                new StatusEffectInstance(
                    StatusEffects.SLOW_FALLING,
                    120,//duration
                    1//amplifier
                )
            );
        }
    }
    
    private boolean shouldAddSlowFalling(Entity entity) {
        if (entity instanceof LivingEntity) {
            if (entity instanceof ServerPlayerEntity) {
                ServerPlayerEntity player = (ServerPlayerEntity) entity;
                if (player.interactionManager.getGameMode() == GameMode.CREATIVE) {
                    return false;
                }
                if (player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA) {
                    return false;
                }
            }
            return true;
        }
        else {
            return false;
        }
    }
}
