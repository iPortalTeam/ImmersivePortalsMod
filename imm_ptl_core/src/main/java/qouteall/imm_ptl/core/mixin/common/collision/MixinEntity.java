package qouteall.imm_ptl.core.mixin.common.collision;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.IPMcHelper;
import qouteall.imm_ptl.core.ducks.IEEntity;
import qouteall.imm_ptl.core.miscellaneous.IPVanillaCopy;
import qouteall.imm_ptl.core.portal.EndPortalEntity;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.teleportation.PortalCollisionHandler;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.my_util.LimitedLogger;

@Mixin(Entity.class)
public abstract class MixinEntity implements IEEntity {
    
    @Nullable
    private PortalCollisionHandler ip_portalCollisionHandler;
    
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
    
    @Shadow
    private Vec3 deltaMovement;
    
    @Shadow
    protected abstract AABB getBoundingBoxForPose(Pose pose);
    
    @Shadow
    @Nullable
    private BlockState feetBlockState;
    @Shadow
    private ChunkPos chunkPosition;
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
            // avoid loading too many chunks in collision calculation and lag the server
            limitedLogger.invoke(() -> {
                Helper.err("Skipping collision calculation because entity moves too fast %s%s%d"
                    .formatted(entity, attemptedMove, entity.level.getGameTime()));
                new Throwable().printStackTrace();
            });
            
            return attemptedMove;
        }
        
        if (!IPGlobal.crossPortalCollision
            || ip_portalCollisionHandler == null
            || !ip_portalCollisionHandler.hasCollisionEntry()
        ) {
            Vec3 normalCollisionResult = collide(attemptedMove);
            return normalCollisionResult;
        }
        
        Vec3 result = ip_portalCollisionHandler.handleCollision(
            (Entity) (Object) this, attemptedMove
        );
        return result;
    }
    
    //don't burn when jumping into end portal
    // TODO make it work for all portals
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
        return ip_getActiveCollisionBox(entity.getBoundingBox());
    }
    
    @Inject(
        method = "checkInsideBlocks",
        at = @At(
            value = "INVOKE_ASSIGN",
            target = "Lnet/minecraft/world/entity/Entity;getBoundingBox()Lnet/minecraft/world/phys/AABB;",
            shift = At.Shift.AFTER
        ),
        locals = LocalCapture.CAPTURE_FAILHARD,
        cancellable = true
    )
    private void onCheckInsideBlocks(CallbackInfo ci, AABB box) {
        if (box == null) {
            ci.cancel();
        }
    }
    
    // avoid suffocation when colliding with a portal on wall
    @Inject(method = "Lnet/minecraft/world/entity/Entity;isInWall()Z", at = @At("HEAD"), cancellable = true)
    private void onIsInsideWall(CallbackInfoReturnable<Boolean> cir) {
        if (isRecentlyCollidingWithPortal()) {
            cir.setReturnValue(false);
        }
    }
    
    // for teleportation debug
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
    
    // when going through a portal with scaling, it sometimes wrongly crouch
    // because of floating-point inaccuracy with bounding box
    // NOTE even it does not crouch, the player is slightly inside the block, and the collision won't always work
    // This is a workaround before finding the correct way of fixing floating-point inaccuracy of collision box after teleportation
//    @ModifyConstant(
//        method = "canEnterPose",
//        constant = @Constant(doubleValue = 1.0E-7)
//    )
//    double modifyShrinkNum(double originalValue) {
//        if (isRecentlyCollidingWithPortal()) {
//            return 0.01;
//        }
//
//        return originalValue;
//    }
    
    /**
     * @author qouteall
     * @reason hard to do without performance loss without overwriting
     */
    @Overwrite
    public boolean canEnterPose(Pose pose) {
        Entity this_ = (Entity) (Object) this;
        AABB activeCollisionBox = ip_getActiveCollisionBox(getBoundingBoxForPose(pose));
        if (activeCollisionBox == null) {
            return true;
        }
        return this.level.noCollision(
            this_,
            activeCollisionBox.deflate(0.1)
        );
    }
    
    // fix climbing onto ladder cross portal
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

//    // debug, should remove in production
//    @Inject(
//        method = "setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V",
//        at = @At("HEAD")
//    )
//    private void onSetDeltaMovement(Vec3 motion, CallbackInfo ci) {
//        Entity this_ = (Entity) (Object) this;
//        if (this_ instanceof LocalPlayer) {
//            if (deltaMovement.y < 0) {
//                if (motion.y > deltaMovement.y) {
//                    Helper.log("debug");
//                }
//            }
//        }
//    }
//
//    // debug, should remove in production
//    @Inject(
//        method = "addDeltaMovement",
//        at = @At("HEAD")
//    )
//    private void onAddDeltaMovement(Vec3 vec3, CallbackInfo ci) {
//        Entity this_ = (Entity) (Object) this;
//        if (this_ instanceof LocalPlayer) {
//            if (deltaMovement.y < 0) {
//                if (vec3.y > 0) {
//                    Helper.log("debug");
//                }
//            }
//        }
//    }
    
    @Override
    public Portal getCollidingPortal() {
        if (ip_portalCollisionHandler == null) {
            return null;
        }
        if (ip_portalCollisionHandler.portalCollisions.isEmpty()) {
            return null;
        }
        return ip_portalCollisionHandler.portalCollisions.get(0).portal;
    }
    
    // this should be called between the range of updating last tick pos and calculating movement
    // because between these two operations, this tick pos is the same as last tick pos
    // CollisionHelper.getStretchedBoundingBox uses the difference between this tick pos and last tick pos
    @Override
    public void tickCollidingPortal(float partialTick) {
        Entity this_ = (Entity) (Object) this;
        
        if (ip_portalCollisionHandler != null) {
            ip_portalCollisionHandler.update(this_);
        }
        
        if (level.isClientSide) {
            IPMcHelper.onClientEntityTick(this_);
        }
    }
    
    @Override
    public void ip_notifyCollidingWithPortal(Entity portal) {
        Entity this_ = (Entity) (Object) this;
        
        if (ip_portalCollisionHandler == null) {
            ip_portalCollisionHandler = new PortalCollisionHandler();
        }
        
        ip_portalCollisionHandler.notifyCollidingWithPortal(this_, ((Portal) portal));
    }
    
    @Override
    public boolean isRecentlyCollidingWithPortal() {
        if (ip_portalCollisionHandler == null) {
            return false;
        }
        return ip_portalCollisionHandler.isRecentlyCollidingWithPortal((Entity) (Object) this);
    }
    
    @Override
    public void portal_unsetRemoved() {
        unsetRemoved();
    }
    
    /**
     * {@link Entity#setPosRaw(double, double, double)}
     */
    @IPVanillaCopy
    @Override
    public void ip_setPositionWithoutTriggeringCallback(Vec3 newPos) {
        double x = newPos.x;
        double y = newPos.y;
        double z = newPos.z;
        
        if (this.position.x != x || this.position.y != y || this.position.z != z) {
            this.position = new Vec3(x, y, z);
            int bx = Mth.floor(x);
            int by = Mth.floor(y);
            int bz = Mth.floor(z);
            if (bx != this.blockPosition.getX()
                || by != this.blockPosition.getY()
                || bz != this.blockPosition.getZ()
            ) {
                this.blockPosition = new BlockPos(bx, by, bz);
                this.feetBlockState = null;
                if (SectionPos.blockToSectionCoord(bx) != this.chunkPosition.x
                    || SectionPos.blockToSectionCoord(bz) != this.chunkPosition.z
                ) {
                    this.chunkPosition = new ChunkPos(this.blockPosition);
                }
            }
        }
    }
    
    @Override
    public void ip_clearCollidingPortal() {
        ip_portalCollisionHandler = null;
    }
    
    @Nullable
    @Override
    public AABB ip_getActiveCollisionBox(AABB originalBox) {
        Entity this_ = (Entity) (Object) this;
        
        if (ip_portalCollisionHandler == null) {
            return originalBox;
        }
        
        return ip_portalCollisionHandler.getActiveCollisionBox(this_, originalBox);
    }
    
    @Nullable
    @Override
    public PortalCollisionHandler ip_getPortalCollisionHandler() {
        return ip_portalCollisionHandler;
    }
    
    @Override
    public PortalCollisionHandler ip_getOrCreatePortalCollisionHandler() {
        if (ip_portalCollisionHandler == null) {
            ip_portalCollisionHandler = new PortalCollisionHandler();
        }
        return ip_portalCollisionHandler;
    }
    
    @Override
    public void ip_setPortalCollisionHandler(@Nullable PortalCollisionHandler handler) {
        ip_portalCollisionHandler = handler;
    }
    
    // don't use game time because client game time may jump due to time synchronization
    private long ip_getStableTiming() {
        return tickCount;
    }
}
