package com.qouteall.immersive_portals.portal;

import com.qouteall.immersive_portals.McHelper;
import net.minecraft.block.Blocks;
import net.minecraft.block.pattern.BlockPattern;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

public class EndPortalEntity extends Portal {
    public static EntityType<EndPortalEntity> entityType;
    
    public EndPortalEntity(
        EntityType<?> entityType_1,
        World world_1
    ) {
        super(entityType_1, world_1);
    }
    
    public static void onEndPortalComplete(ServerWorld world, BlockPattern.Result pattern) {
        Portal portal = new EndPortalEntity(entityType,world);
        
        Vec3d center = new Vec3d(pattern.getFrontTopLeft()).add(-1.5, 0.5, -1.5);
        portal.updatePosition(center.x, center.y, center.z);
        
        portal.destination = new Vec3d(0, 120, 0);
        
        portal.dimensionTo = DimensionType.THE_END;
        
        portal.axisW = new Vec3d(0, 0, 1);
        portal.axisH = new Vec3d(1, 0, 0);
        
        portal.width = 3;
        portal.height = 3;
        
        world.spawnEntity(portal);
    }
    
    @Override
    public void onEntityTeleportedOnServer(Entity entity) {
        if (shouldAddSlowFalling(entity)) {
            LivingEntity livingEntity = (LivingEntity) entity;
            livingEntity.addStatusEffect(
                new StatusEffectInstance(
                    StatusEffects.SLOW_FALLING,
                    120,//duration
                    1//amplifier
                )
            );
        }
        if (entity instanceof ServerPlayerEntity) {
            generateObsidianPlatform();
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
    
    private void generateObsidianPlatform() {
        ServerWorld endWorld = McHelper.getServer().getWorld(DimensionType.THE_END);
        BlockPos spawnPoint = endWorld.getForcedSpawnPoint();
        
        int int_1 = spawnPoint.getX();
        int int_2 = spawnPoint.getY() - 1;
        int int_3 = spawnPoint.getZ();
        
        for (int int_6 = -2; int_6 <= 2; ++int_6) {
            for (int int_7 = -2; int_7 <= 2; ++int_7) {
                for (int int_8 = -1; int_8 < 3; ++int_8) {
                    int int_9 = int_1 + int_7 * 1 + int_6 * 0;
                    int int_10 = int_2 + int_8;
                    int int_11 = int_3 + int_7 * 0 - int_6 * 1;
                    boolean boolean_1 = int_8 < 0;
                    endWorld.setBlockState(
                        new BlockPos(int_9, int_10, int_11),
                        boolean_1 ? Blocks.OBSIDIAN.getDefaultState() :
                            Blocks.AIR.getDefaultState()
                    );
                }
            }
        }
    }
}
