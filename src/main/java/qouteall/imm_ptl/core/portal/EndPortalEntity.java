package qouteall.imm_ptl.core.portal;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
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
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.ducks.IEEntity;
import qouteall.imm_ptl.core.mixin.common.miscellaneous.IEEndDragonFight;
import qouteall.imm_ptl.core.portal.animation.NormalAnimation;
import qouteall.imm_ptl.core.portal.animation.RotationAnimation;
import qouteall.imm_ptl.core.portal.shape.BoxPortalShape;
import qouteall.q_misc_util.Helper;

import java.util.Objects;

public class EndPortalEntity extends Portal {
    private static final Logger LOGGER = LogManager.getLogger(EndPortalEntity.class);
    
    public static final EntityType<EndPortalEntity> ENTITY_TYPE =
        createPortalEntityType(EndPortalEntity::new);
    public static final String PORTAL_TAG_VIEW_BOX = "view_box";
    
    private static final double BOX_PORTAL_SIDE_LEN = 3;
    private static final double BOX_PORTAL_HEIGHT = 1.5;
    
    public EndPortalEntity(
        EntityType<?> entityType,
        Level world
    ) {
        super(entityType, world);
        
        setCrossPortalCollisionEnabled(false);
    }
    
    public static void onEndPortalComplete(ServerLevel world, Vec3 portalCenter) {
        ServerLevel endDim = world.getServer().getLevel(Level.END);
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
            LOGGER.error("End portal mode abnormal");
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
        Portal portal = new EndPortalEntity(EndPortalEntity.ENTITY_TYPE, world);
        
        portal.setPos(portalCenter.x, portalCenter.y, portalCenter.z);
        
        portal.setDestination(destination);
        
        portal.setDestDim(Level.END);
        
        portal.setAxisW(new Vec3(0, 0, 1));
        portal.setAxisH(new Vec3(1, 0, 0));
        
        portal.setWidth(3);
        portal.setHeight(3);
        
        world.addFreshEntity(portal);
    }
    
    private static void generateScaledViewEndPortal(
        ServerLevel world, Vec3 frameCenter, boolean hasAnimation
    ) {
        ServerLevel endWorld = world.getServer().getLevel(Level.END);
        Validate.notNull(endWorld, "End dimension is not loaded");
        
        double sideLen = BOX_PORTAL_SIDE_LEN;
        double otherSideSideLen = 18 * 16; // 18 chunks
        double height = BOX_PORTAL_HEIGHT;
        final Vec3 viewBoxSize = new Vec3(sideLen, height, sideLen);
        final double scale = otherSideSideLen / sideLen;
        
        AABB thisSideBox = Helper.getBoxByBottomPosAndSize(
            frameCenter.add(0, 0.2, 0), viewBoxSize
        );
        AABB otherSideBox = Helper.getBoxByBottomPosAndSize(
            new Vec3(0, 0, 0),
            viewBoxSize.scale(scale)
        );
        
        Vec3 portalCenter = thisSideBox.getCenter();
        
        EndPortalEntity portal = EndPortalEntity.ENTITY_TYPE.create(world);
        assert portal != null;
        
        portal.setOriginPos(portalCenter);
        portal.setDestDim(endWorld.dimension());
        portal.setDestination(otherSideBox.getCenter());
        
        portal.setAxisW(new Vec3(1, 0, 0));
        portal.setAxisH(new Vec3(0, 1, 0));
        portal.setWidth(sideLen);
        portal.setHeight(height);
        portal.setThickness(sideLen);
        
        portal.setPortalShape(BoxPortalShape.FACING_OUTWARDS);
        
        portal.setScaling(scale);
        portal.setTeleportChangesScale(false);
        portal.portalTag = PORTAL_TAG_VIEW_BOX;
        portal.setInteractable(false);
        portal.setCrossPortalCollisionEnabled(false);
        portal.setFuseView(true);
        
        if (hasAnimation) {
            Vec3 rotationCenter = portalCenter;
            portal.addThisSideAnimationDriver(
                new RotationAnimation.Builder()
                    .setDegreesPerTick(0.5)
                    .setInitialOffset(Vec3.ZERO)
                    .setRotationAxis(new Vec3(0, 1, 0))
                    .setStartGameTime(world.getGameTime())
                    .setEndGameTime(Long.MAX_VALUE)
                    .build()
            );
            
            portal.addThisSideAnimationDriver(
                NormalAnimation.createOscillationAnimation(
                    new Vec3(0, 0.3, 0),
                    100,
                    world.getGameTime()
                )
            );
        }
        
        McHelper.spawnServerEntity(portal);
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
            
            if (getPortalShape() instanceof BoxPortalShape) {
                Vec3 cameraPosVec = player.getEyePosition(1);
                double dist = this.getDistanceToNearestPointInPortal(cameraPosVec);
                if (dist < 1 && isInBoxPortalTeleportataionRange(cameraPosVec)) {
//                    double mul = Math.min(dist / 2, 0.1);
                    double mul = 0.5;
                    player.setDeltaMovement(
                        player.getDeltaMovement().x * mul,
                        player.getDeltaMovement().y * mul,
                        player.getDeltaMovement().z * mul
                    );
                }
            }
            else if (getNormal().y > 0.5) {
                // legacy behavior
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
            setFuseView(true);
        }
    }
    
    @Override
    public void onEntityTeleportedOnServer(Entity entity) {
        super.onEntityTeleportedOnServer(entity);
        
        if (shouldAddSlowFalling(entity)) {
            int duration = 200;
            
            if (isViewBoxPortal()) {
                duration = 400;
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
        
        MinecraftServer server = getServer();
        assert server != null;
        ServerLevel endWorld = server.getLevel(Level.END);
        if (endWorld != null) {
            ServerLevel.makeObsidianPlatform(endWorld);
        }
    }
    
    private boolean isViewBoxPortal() {
        return Objects.equals(this.portalTag, PORTAL_TAG_VIEW_BOX);
    }
    
    // avoid scale box portal to transform velocity
    @Override
    public Vec3 transformVelocityRelativeToPortal(
        Vec3 originalVelocityRelativeToPortal, Entity entity, Vec3 oldEntityPos
    ) {
        if (isViewBoxPortal()) {
            Vec3 offset = oldEntityPos.subtract(getOriginPos());
            double threshold = 1.0;
            double horizontalDistanceSq = offset.x * offset.x + offset.z * offset.z;
            
            if (horizontalDistanceSq > threshold * threshold) {
                // if far from center, it will likely not land to the end island
                // reset velocity to give player the time to move
                return Vec3.ZERO;
            }
        }
        
        return super.transformVelocityRelativeToPortal(originalVelocityRelativeToPortal, entity, oldEntityPos);
    }
    
    // arrows cannot go through end portal
    // avoid easily snipping end crystals
    @Override
    public boolean canTeleportEntity(Entity entity) {
        if (entity instanceof Arrow) {
            return false;
        }
        
        if (getPortalShape() instanceof BoxPortalShape) {
            if (!isInBoxPortalTeleportataionRange(entity.getEyePosition(1))) {
                return false;
            }
        }
        
        return super.canTeleportEntity(entity);
    }
    
    private boolean shouldAddSlowFalling(Entity entity) {
        if (entity instanceof LivingEntity) {
            if (entity instanceof ServerPlayer player) {
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
        // make obsidian platform not generated too late so that player falls down
        if (!level().isClientSide()) {
            if (entity instanceof ServerPlayer) {
                if (IPGlobal.endPortalMode == IPGlobal.EndPortalMode.toObsidianPlatform) {
                    MinecraftServer server = getServer();
                    assert server != null;
                    ServerLevel endWorld = server.getLevel(Level.END);
                    if (endWorld != null) {
                        ServerLevel.makeObsidianPlatform(endWorld);
                    }
                }
            }
        }
    }
    
    public boolean isInBoxPortalTeleportataionRange(Vec3 pos) {
        Vec3 offset = pos.subtract(getOriginPos());
        
        double threshold = BOX_PORTAL_SIDE_LEN / 2;
        double horizontalDistanceSq = offset.x * offset.x + offset.z * offset.z;
        return horizontalDistanceSq <= threshold * threshold;
    }
    
}
