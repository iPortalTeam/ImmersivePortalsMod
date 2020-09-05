package com.qouteall.immersive_portals.portal;

import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;

import java.util.Objects;

public class EndPortalEntity extends Portal {
    public static EntityType<EndPortalEntity> entityType;
    
    // only used by scaled view type end portal
    private EndPortalEntity clientFakedReversePortal;
    
    public EndPortalEntity(
        EntityType<?> entityType_1,
        World world_1
    ) {
        super(entityType_1, world_1);
    }
    
    public static void onEndPortalComplete(ServerWorld world, Vec3d portalCenter) {
        if (Global.endPortalMode == Global.EndPortalMode.normal) {
            generateClassicalEndPortal(world, new Vec3d(0, 120, 0), portalCenter);
        }
        else if (Global.endPortalMode == Global.EndPortalMode.toObsidianPlatform) {
            BlockPos endSpawnPos = ServerWorld.END_SPAWN_POS;
            generateClassicalEndPortal(world,
                Vec3d.ofCenter(endSpawnPos).add(0, 1, 0), portalCenter
            );
        }
        else if (Global.endPortalMode == Global.EndPortalMode.scaledView) {
            generateScaledViewEndPortal(world, portalCenter);
        }
        else {
            Helper.err("End portal mode abnormal");
        }
    }
    
    private static void generateClassicalEndPortal(ServerWorld world, Vec3d destination, Vec3d portalCenter) {
        Portal portal = new EndPortalEntity(entityType, world);
        
        portal.updatePosition(portalCenter.x, portalCenter.y, portalCenter.z);
        
        portal.destination = destination;
        
        portal.dimensionTo = World.END;
        
        portal.axisW = new Vec3d(0, 0, 1);
        portal.axisH = new Vec3d(1, 0, 0);
        
        portal.width = 3;
        portal.height = 3;
        
        world.spawnEntity(portal);
    }
    
    private static void generateScaledViewEndPortal(ServerWorld world, Vec3d portalCenter) {
        ServerWorld endWorld = McHelper.getServerWorld(World.END);
        
        double d = 3;
        final Vec3d viewBoxSize = new Vec3d(d, 1.4, d);
        final double scale = 270 / d;
        
        Box thisSideBox = Helper.getBoxByBottomPosAndSize(
            portalCenter.add(0, 0, 0), viewBoxSize
        );
        Box otherSideBox = Helper.getBoxByBottomPosAndSize(
            new Vec3d(0, 0, 0),
            viewBoxSize.multiply(scale)
        );
        
        for (Direction direction : Direction.values()) {
            Portal portal = PortalManipulation.createOrthodoxPortal(
                EndPortalEntity.entityType,
                world, endWorld,
                direction, Helper.getBoxSurface(thisSideBox, direction),
                Helper.getBoxSurface(otherSideBox, direction).getCenter()
            );
            portal.scaling = scale;
            portal.teleportChangesScale = false;
            portal.extension.adjustPositionAfterTeleport = true;
            portal.portalTag = "view_box";
            portal.extension.motionAffinity = -0.9;
            //creating a new entity type needs registering
            //it's easier to discriminate it by portalTag
            
            McHelper.spawnServerEntityToUnloadedArea(portal);
        }
    }
    
    @Override
    public void tick() {
        super.tick();
        
        if (world.isClient()) {
            tickClient();
        }
    }
    
    @Environment(EnvType.CLIENT)
    private void tickClient() {
        if (Objects.equals(portalTag, "view_box")) {
            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            if (player == null) {
                return;
            }
            if (player.world == this.world && player.getPos().squaredDistanceTo(getPos()) < 10 * 10) {
                if (clientFakedReversePortal == null) {
                    // client only faked portal
                    clientFakedReversePortal =
                        PortalManipulation.createReversePortal(this, EndPortalEntity.entityType);
                    
                    clientFakedReversePortal.teleportable = false;
                    
                    clientFakedReversePortal.portalTag = "view_box_faked_reverse";
                    
                    clientFakedReversePortal.clientFakedReversePortal = this;
                    
                    ((ClientWorld) getDestinationWorld()).addEntity(
                        clientFakedReversePortal.getEntityId(),
                        clientFakedReversePortal
                    );
                }
            }
            else {
                if (clientFakedReversePortal != null) {
                    clientFakedReversePortal.remove();
                    clientFakedReversePortal = null;
                }
            }
        }
        else if (Objects.equals(portalTag, "view_box_faked_reverse")) {
            if (clientFakedReversePortal.removed) {
                remove();
            }
        }
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
    
    @Override
    public void transformVelocity(Entity entity) {
//        Vec3d velocity = entity.getVelocity();
//        entity.setVelocity(velocity.x, 0, velocity.z);
    }
    
    // arrows cannot go through end portal
    // avoid easily snipping end crystals
    @Override
    public boolean canTeleportEntity(Entity entity) {
        if (entity instanceof ArrowEntity) {
            return false;
        }
        return super.canTeleportEntity(entity);
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
        ServerWorld endWorld = McHelper.getServer().getWorld(World.END);
        
        ServerWorld.createEndSpawnPlatform(endWorld);
    }
}
