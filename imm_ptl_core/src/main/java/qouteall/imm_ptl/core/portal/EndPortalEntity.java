package qouteall.imm_ptl.core.portal;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
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
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.ducks.IEEntity;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.MiscHelper;

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
        
        renderingMergable = true;
        hasCrossPortalCollision = false;
    }
    
    public static void onEndPortalComplete(ServerWorld world, Vec3d portalCenter) {
        if (IPGlobal.endPortalMode == IPGlobal.EndPortalMode.normal) {
            generateClassicalEndPortal(world, new Vec3d(0, 120, 0), portalCenter);
        }
        else if (IPGlobal.endPortalMode == IPGlobal.EndPortalMode.toObsidianPlatform) {
            BlockPos endSpawnPos = ServerWorld.END_SPAWN_POS;
            generateClassicalEndPortal(world,
                Vec3d.ofCenter(endSpawnPos).add(0, 1, 0), portalCenter
            );
        }
        else if (IPGlobal.endPortalMode == IPGlobal.EndPortalMode.scaledView) {
            generateScaledViewEndPortal(world, portalCenter);
        }
        else {
            Helper.err("End portal mode abnormal");
        }
        
        // for toObsidianPlatform mode, if the platform does not get generated before
        // going through portal, the player may fall into void
        generateObsidianPlatform();
    }
    
    private static void generateClassicalEndPortal(ServerWorld world, Vec3d destination, Vec3d portalCenter) {
        Portal portal = new EndPortalEntity(entityType, world);
        
        portal.setPosition(portalCenter.x, portalCenter.y, portalCenter.z);
        
        portal.setDestination(destination);
        
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
        final Vec3d viewBoxSize = new Vec3d(d, 1.2, d);
        final double scale = 280 / d;
        
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
            PortalExtension.get(portal).adjustPositionAfterTeleport = true;
            portal.portalTag = "view_box";
            //creating a new entity type needs registering
            //it's easier to discriminate it by portalTag
            
            McHelper.spawnServerEntity(portal);
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
            if (getNormal().y > 0.5) {
                if (((IEEntity) player).getCollidingPortal() == this) {
                    Vec3d cameraPosVec = player.getCameraPosVec(1);
                    double dist = this.getDistanceToNearestPointInPortal(cameraPosVec);
                    if (dist < 1) {
                        double mul = dist / 2 + 0.1;
                        player.setVelocity(
                            player.getVelocity().x * mul,
                            player.getVelocity().y * mul,
                            player.getVelocity().z * mul
                        );
                    }
                }
            }
            fuseView = true;
        }
        else if (Objects.equals(portalTag, "view_box_faked_reverse")) {
            if (clientFakedReversePortal.isRemoved()) {
                remove(RemovalReason.KILLED);
            }
        }
    }
    
    @Override
    public void onEntityTeleportedOnServer(Entity entity) {
        super.onEntityTeleportedOnServer(entity);
        
        if (shouldAddSlowFalling(entity)) {
            int duration = 120;
            
            if (Objects.equals(this.portalTag, "view_box")) {
                duration = 200;
            }
            
            LivingEntity livingEntity = (LivingEntity) entity;
            livingEntity.addStatusEffect(
                new StatusEffectInstance(
                    StatusEffects.SLOW_FALLING,
                    duration,//duration
                    1//amplifier
                )
            );
        }
        
        generateObsidianPlatform();
    }
    
    @Override
    public void transformVelocity(Entity entity) {
        // avoid scale box portal to transform velocity
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
    
    // if the bounding box is too small
    // grouping will fail
    @Override
    public boolean shouldLimitBoundingBox() {
        return false;
    }
    
    private static void generateObsidianPlatform() {
        ServerWorld endWorld = MiscHelper.getServer().getWorld(World.END);
        
        ServerWorld.createEndSpawnPlatform(endWorld);
    }
    
    @Override
    public void onCollidingWithEntity(Entity entity) {
        // fix https://github.com/qouteall/ImmersivePortalsMod/issues/698
        // maybe allows easier farming of obsidian
        if (!world.isClient()) {
            if (entity instanceof ServerPlayerEntity) {
                if (IPGlobal.endPortalMode == IPGlobal.EndPortalMode.toObsidianPlatform) {
                    generateObsidianPlatform();
                }
            }
        }
    }
}
