package qouteall.imm_ptl.core.portal;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.ducks.IEEntity;
import qouteall.imm_ptl.core.mixin.common.miscellaneous.IEEndDragonFight;
import qouteall.imm_ptl.core.portal.animation.NormalAnimation;
import qouteall.imm_ptl.core.portal.animation.RotationAnimation;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.MiscHelper;

import java.util.Objects;

public class EndPortalEntity extends Portal {
    private static final Logger LOGGER = LogManager.getLogger(EndPortalEntity.class);
    
    public static final EntityType<EndPortalEntity> entityType =
        createPortalEntityType(EndPortalEntity::new);
    public static final String PORTAL_TAG_VIEW_BOX = "view_box";
    
    // only used by scaled view type end portal
    private EndPortalEntity clientFakedReversePortal;
    
    public EndPortalEntity(
        EntityType<?> entityType,
        Level world
    ) {
        super(entityType, world);
        
        renderingMergable = true;
        hasCrossPortalCollision = false;
    }
    
    public static void onEndPortalComplete(ServerLevel world, Vec3 portalCenter) {
        ServerLevel endDim = MiscHelper.getServer().getLevel(Level.END);
        if (endDim == null) {
            // there is no end dimension (custom preset can remove the end dimension)
            return;
        }
        
        IPGlobal.EndPortalMode endPortalMode = IPGlobal.endPortalMode;
        if (endPortalMode == IPGlobal.EndPortalMode.normal) {
            generateClassicalEndPortal(world, new Vec3(0, 120, 0), portalCenter);
        }
        else if (endPortalMode == IPGlobal.EndPortalMode.toObsidianPlatform) {
            BlockPos endSpawnPos = ServerLevel.END_SPAWN_POINT;
            generateClassicalEndPortal(world,
                Vec3.atCenterOf(endSpawnPos).add(0, 1, 0), portalCenter
            );
        }
        else if (endPortalMode == IPGlobal.EndPortalMode.scaledView) {
            generateScaledViewEndPortal(world, portalCenter, false);
        }
        else if (endPortalMode == IPGlobal.EndPortalMode.scaledViewRotating) {
            generateScaledViewEndPortal(world, portalCenter, true);
        }
        else {
            Helper.err("End portal mode abnormal");
        }
        
        // for toObsidianPlatform mode, if the platform does not get generated before
        // going through portal, the player may fall into void
        ServerLevel.makeObsidianPlatform(world);
        
        // update dragon fight info
        EndDragonFight dragonFight = world.getDragonFight();
        if (dragonFight == null) {
            return;
        }
        if (((IEEndDragonFight) dragonFight).ip_getNeedsStateScanning()) {
            ((IEEndDragonFight) dragonFight).ip_scanState();
        }
    }
    
    private static void generateClassicalEndPortal(ServerLevel world, Vec3 destination, Vec3 portalCenter) {
        Portal portal = new EndPortalEntity(entityType, world);
        
        portal.setPos(portalCenter.x, portalCenter.y, portalCenter.z);
        
        portal.setDestination(destination);
        
        portal.dimensionTo = Level.END;
        
        portal.axisW = new Vec3(0, 0, 1);
        portal.axisH = new Vec3(1, 0, 0);
        
        portal.width = 3;
        portal.height = 3;
        
        world.addFreshEntity(portal);
    }
    
    private static void generateScaledViewEndPortal(
        ServerLevel world, Vec3 portalCenter, boolean hasAnimation
    ) {
        ServerLevel endWorld = McHelper.getServerWorld(Level.END);
        
        double sideLen = 3;
        double height = 1.5;
        final Vec3 viewBoxSize = new Vec3(sideLen, height, sideLen);
        final double scale = 280 / sideLen;
        
        AABB thisSideBox = Helper.getBoxByBottomPosAndSize(
            portalCenter.add(0, 0.2, 0), viewBoxSize
        );
        AABB otherSideBox = Helper.getBoxByBottomPosAndSize(
            new Vec3(0, 0, 0),
            viewBoxSize.scale(scale)
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
            portal.portalTag = PORTAL_TAG_VIEW_BOX;
            portal.setInteractable(false);
            //creating a new entity type needs registering
            //it's easier to discriminate it by portalTag
            
            if (hasAnimation) {
                Vec3 rotationCenter = Vec3.ZERO;
                portal.addOtherSideAnimationDriver(
                    new RotationAnimation.Builder()
                        .setDegreesPerTick(0.5)
                        .setInitialOffset(portal.destination.subtract(rotationCenter))
                        .setRotationAxis(new Vec3(0, 1, 0))
                        .setStartGameTime(world.getGameTime())
                        .setEndGameTime(Long.MAX_VALUE)
                        .build()
                );
                
                portal.addOtherSideAnimationDriver(
                    NormalAnimation.createOscillationAnimation(
                        new Vec3(0, 10, 0),
                        100,
                        world.getGameTime()
                    )
                );
            }
            
            McHelper.spawnServerEntity(portal);
        }
    }
    
    @Override
    public void tick() {
        super.tick();
        
        if (level().isClientSide()) {
            tickClient();
        }
    }
    
    @Environment(EnvType.CLIENT)
    private void tickClient() {
        if (isViewBoxPortal()) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) {
                return;
            }
            if (getNormal().y > 0.5) {
                if (((IEEntity) player).ip_getCollidingPortal() == this) {
                    Vec3 cameraPosVec = player.getEyePosition(1);
                    double dist = this.getDistanceToNearestPointInPortal(cameraPosVec);
                    if (dist < 1) {
                        double mul = dist / 2 + 0.1;
                        player.setDeltaMovement(
                            player.getDeltaMovement().x * mul,
                            player.getDeltaMovement().y * mul,
                            player.getDeltaMovement().z * mul
                        );
                    }
                }
            }
            fuseView = true;
        }
    }
    
    @Override
    public void onEntityTeleportedOnServer(Entity entity) {
        super.onEntityTeleportedOnServer(entity);
        
        if (shouldAddSlowFalling(entity)) {
            int duration = 200;
            
            if (isViewBoxPortal()) {
                duration = 300;
            }
            
            LivingEntity livingEntity = (LivingEntity) entity;
            livingEntity.addEffect(
                new MobEffectInstance(
                    MobEffects.SLOW_FALLING,
                    duration,//duration
                    1//amplifier
                )
            );
        }
        
        ServerLevel endWorld = MiscHelper.getServer().getLevel(Level.END);
        
        ServerLevel.makeObsidianPlatform(endWorld);
    }
    
    private boolean isViewBoxPortal() {
        return Objects.equals(this.portalTag, PORTAL_TAG_VIEW_BOX);
    }
    
    // avoid scale box portal to transform velocity
    @Override
    public Vec3 transformVelocityRelativeToPortal(Vec3 originalVelocityRelativeToPortal, Entity entity) {
        if (isViewBoxPortal()) {
            return Vec3.ZERO;
        }
        
        return super.transformVelocityRelativeToPortal(originalVelocityRelativeToPortal, entity);
    }
    
    // arrows cannot go through end portal
    // avoid easily snipping end crystals
    @Override
    public boolean canTeleportEntity(Entity entity) {
        if (entity instanceof Arrow) {
            return false;
        }
        return super.canTeleportEntity(entity);
    }
    
    private boolean shouldAddSlowFalling(Entity entity) {
        if (entity instanceof LivingEntity) {
            if (entity instanceof ServerPlayer) {
                ServerPlayer player = (ServerPlayer) entity;
                if (player.gameMode.getGameModeForPlayer() == GameType.CREATIVE) {
                    return false;
                }
                if (player.getItemBySlot(EquipmentSlot.CHEST).getItem() == Items.ELYTRA) {
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
    
    @Override
    public void onCollidingWithEntity(Entity entity) {
        // fix https://github.com/qouteall/ImmersivePortalsMod/issues/698
        // maybe allows easier farming of obsidian
        if (!level().isClientSide()) {
            if (entity instanceof ServerPlayer) {
                if (IPGlobal.endPortalMode == IPGlobal.EndPortalMode.toObsidianPlatform) {
                    ServerLevel endWorld = MiscHelper.getServer().getLevel(Level.END);
                    
                    ServerLevel.makeObsidianPlatform(endWorld);
                }
            }
        }
    }
    
}
