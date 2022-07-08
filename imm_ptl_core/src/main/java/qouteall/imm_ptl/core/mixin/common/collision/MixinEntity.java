package qouteall.imm_ptl.core.mixin.common.collision;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.IPMcHelper;
import qouteall.imm_ptl.core.ducks.IEEntity;
import qouteall.imm_ptl.core.portal.EndPortalEntity;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.teleportation.CollisionHelper;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.my_util.LimitedLogger;

@Mixin(Entity.class)
public abstract class MixinEntity implements IEEntity {
    
    private Entity collidingPortal;
    private long collidingPortalActiveTickTime;
    
    @Shadow
    public abstract AABB getBoundingBox();
    
    @Shadow
    public Level level;
    
    @Shadow
    public abstract void setBoundingBox(AABB box_1);
    
    @Shadow
    protected abstract Vec3 collide(Vec3 vec3d_1);
    
    @Shadow
    public abstract Component getName();
    
    @Shadow
    public abstract double getX();
    
    @Shadow
    public abstract double getY();
    
    @Shadow
    public abstract double getZ();
    
    @Shadow
    protected abstract BlockPos getOnPos();
    
    @Shadow
    public boolean blocksBuilding;
    
    @Shadow
    public int tickCount;
    
    @Shadow
    public abstract Vec3 getDeltaMovement();
    
    @Shadow
    protected abstract void unsetRemoved();
    
    @Shadow
    private Vec3 position;
    
    @Shadow
    private BlockPos blockPosition;
    
    //maintain collidingPortal field
    @Inject(method = "Lnet/minecraft/world/entity/Entity;tick()V", at = @At("HEAD"))
    private void onTicking(CallbackInfo ci) {
        tickCollidingPortal(1);
    }
    
    private static final LimitedLogger limitedLogger = new LimitedLogger(20);
    
    @Redirect(
        method = "Lnet/minecraft/world/entity/Entity;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;"
        )
    )
    private Vec3 redirectHandleCollisions(Entity entity, Vec3 attemptedMove) {
        if (!IPGlobal.enableServerCollision) {
            if (!entity.level.isClientSide()) {
                if (entity instanceof Player) {
                    return attemptedMove;
                }
                else {
                    return Vec3.ZERO;
                }
            }
        }
        
        if (attemptedMove.lengthSqr() > 60 * 60) {
            limitedLogger.invoke(() -> {
                Helper.err("Entity moves too fast " + entity + attemptedMove + entity.level.getGameTime());
                new Throwable().printStackTrace();
            });

//            if (entity instanceof ServerPlayerEntity) {
//                ServerTeleportationManager.sendPositionConfirmMessage(((ServerPlayerEntity) entity));
//                Helper.log("position confirm message sent " + entity);
//            }
            
            return attemptedMove;
        }
        
        if (getDeltaMovement().lengthSqr() > 2) {
            CollisionHelper.updateCollidingPortalNow(entity);
        }
        
        if (collidingPortal == null ||
//            entity.hasPassengers() ||
//            entity.hasVehicle() ||
            !IPGlobal.crossPortalCollision
        ) {
            return collide(attemptedMove);
        }
        
        Vec3 result = CollisionHelper.handleCollisionHalfwayInPortal(
            (Entity) (Object) this,
            attemptedMove,
            getCollidingPortal()
        );
        return result;
    }
    
    //don't burn when jumping into end portal
    @Inject(
        method = "Lnet/minecraft/world/entity/Entity;fireImmune()Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onIsFireImmune(CallbackInfoReturnable<Boolean> cir) {
        if (getCollidingPortal() instanceof EndPortalEntity) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }
    
    @Redirect(
        method = "Lnet/minecraft/world/entity/Entity;checkInsideBlocks()V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;getBoundingBox()Lnet/minecraft/world/phys/AABB;"
        )
    )
    private AABB redirectBoundingBoxInCheckingBlockCollision(Entity entity) {
        return CollisionHelper.getActiveCollisionBox(entity);
    }
    
    // avoid suffocation when colliding with a portal on wall
    @Inject(method = "Lnet/minecraft/world/entity/Entity;isInWall()Z", at = @At("HEAD"), cancellable = true)
    private void onIsInsideWall(CallbackInfoReturnable<Boolean> cir) {
        if (isRecentlyCollidingWithPortal()) {
            cir.setReturnValue(false);
        }
    }
    
    //for teleportation debug
    @Inject(
        method = "Lnet/minecraft/world/entity/Entity;setPosRaw(DDD)V",
        at = @At("HEAD")
    )
    private void onSetPos(double nx, double ny, double nz, CallbackInfo ci) {
        if (((Object) this) instanceof ServerPlayer) {
            if (IPGlobal.teleportationDebugEnabled) {
                if (Math.abs(getX() - nx) > 10 ||
                    Math.abs(getY() - ny) > 10 ||
                    Math.abs(getZ() - nz) > 10
                ) {
                    Helper.log(String.format(
                        "%s %s teleported from %s %s %s to %s %s %s",
                        getName().getContents(),
                        level.dimension(),
                        (int) getX(), (int) getY(), (int) getZ(),
                        (int) nx, (int) ny, (int) nz
                    ));
                    new Throwable().printStackTrace();
                }
            }
        }
    }
    
    // Avoid instant crouching when crossing a scaling portal
    @Inject(method = "Lnet/minecraft/world/entity/Entity;canEnterPose(Lnet/minecraft/world/entity/Pose;)Z", at = @At("HEAD"), cancellable = true)
    private void onWouldPoseNotCollide(Pose pose, CallbackInfoReturnable<Boolean> cir) {
        if (isRecentlyCollidingWithPortal()) {
            cir.setReturnValue(true);
        }
    }
    
    //fix climbing onto ladder cross portal
    @Inject(method = "Lnet/minecraft/world/entity/Entity;getFeetBlockState()Lnet/minecraft/world/level/block/state/BlockState;", at = @At("HEAD"), cancellable = true)
    private void onGetBlockState(CallbackInfoReturnable<BlockState> cir) {
        Portal collidingPortal = ((IEEntity) this).getCollidingPortal();
        Entity this_ = (Entity) (Object) this;
        if (collidingPortal != null) {
            if (collidingPortal.getNormal().y > 0) {
                BlockPos remoteLandingPos = new BlockPos(
                    collidingPortal.transformPoint(this_.position())
                );
                
                Level destinationWorld = collidingPortal.getDestinationWorld();
                
                if (destinationWorld.hasChunkAt(remoteLandingPos)) {
                    BlockState result = destinationWorld.getBlockState(remoteLandingPos);
                    
                    if (!result.isAir()) {
                        cir.setReturnValue(result);
                        cir.cancel();
                    }
                }
            }
        }
    }
    
    @Override
    public Portal getCollidingPortal() {
        return ((Portal) collidingPortal);
    }
    
    @Override
    public void tickCollidingPortal(float tickDelta) {
        Entity this_ = (Entity) (Object) this;
        
        if (collidingPortal != null) {
            if (collidingPortal.level != level) {
                collidingPortal = null;
            }
            else {
                AABB stretchedBoundingBox = CollisionHelper.getStretchedBoundingBox(this_);
                if (!stretchedBoundingBox.inflate(0.5).intersects(collidingPortal.getBoundingBox())) {
                    collidingPortal = null;
                }
            }
            
            if (Math.abs(tickCount - collidingPortalActiveTickTime) >= 3) {
                collidingPortal = null;
            }
        }
        
        if (level.isClientSide) {
            IPMcHelper.onClientEntityTick(this_);
        }
    }
    
    @Override
    public void notifyCollidingWithPortal(Entity portal) {
        Entity this_ = (Entity) (Object) this;
        
        collidingPortal = portal;
        collidingPortalActiveTickTime = tickCount;//world time may jump due to time synchroization
        ((Portal) portal).onCollidingWithEntity(this_);
    }
    
    @Override
    public boolean isRecentlyCollidingWithPortal() {
        return (tickCount - collidingPortalActiveTickTime) < 20;
    }
    
    @Override
    public void portal_unsetRemoved() {
        unsetRemoved();
    }
    
    @Override
    public void ip_setPositionWithoutTriggeringCallback(Vec3 newPos) {
        this.position = newPos;
        this.blockPosition = new BlockPos(newPos);
    }
}
