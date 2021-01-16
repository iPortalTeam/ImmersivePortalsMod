package com.qouteall.immersive_portals.mixin.common.collision;

import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.ducks.IEEntity;
import com.qouteall.immersive_portals.portal.EndPortalEntity;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.teleportation.CollisionHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class MixinEntity implements IEEntity {
    
    private Entity collidingPortal;
    private long collidingPortalActiveTickTime;
    
    @Shadow
    public abstract Box getBoundingBox();
    
    @Shadow
    public World world;
    
    @Shadow
    public abstract void setBoundingBox(Box box_1);
    
    @Shadow
    protected abstract Vec3d adjustMovementForCollisions(Vec3d vec3d_1);
    
    @Shadow
    public abstract Text getName();
    
    @Shadow
    public abstract double getX();
    
    @Shadow
    public abstract double getY();
    
    @Shadow
    public abstract double getZ();
    
    @Shadow
    protected abstract BlockPos getLandingPos();
    
    @Shadow
    public boolean inanimate;
    
    @Shadow
    private boolean chunkPosUpdateRequested;
    
    //maintain collidingPortal field
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTicking(CallbackInfo ci) {
        tickCollidingPortal(1);
    }
    
    @Redirect(
        method = "move",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/Entity;adjustMovementForCollisions(Lnet/minecraft/util/math/Vec3d;)Lnet/minecraft/util/math/Vec3d;"
        )
    )
    private Vec3d redirectHandleCollisions(Entity entity, Vec3d attemptedMove) {
        if (attemptedMove.lengthSquared() > 1600) {
            Helper.err("Entity moves too fast " + entity + attemptedMove);
            new Throwable().printStackTrace();
            return attemptedMove;
        }
        
        if (collidingPortal == null ||
            entity.hasPassengers() ||
            entity.hasVehicle() ||
            !Global.crossPortalCollision
        ) {
            return adjustMovementForCollisions(attemptedMove);
        }
        
        Vec3d result = CollisionHelper.handleCollisionHalfwayInPortal(
            (Entity) (Object) this,
            attemptedMove,
            getCollidingPortal(),
            attemptedMove1 -> adjustMovementForCollisions(attemptedMove1)
        );
        return result;
    }
    
    //don't burn when jumping into end portal
    @Inject(
        method = "isFireImmune",
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
        method = "checkBlockCollision",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/Entity;getBoundingBox()Lnet/minecraft/util/math/Box;"
        )
    )
    private Box redirectBoundingBoxInCheckingBlockCollision(Entity entity) {
        return CollisionHelper.getActiveCollisionBox(entity);
    }
    
    // avoid suffocation when colliding with a portal on wall
    @Inject(method = "isInsideWall", at = @At("HEAD"), cancellable = true)
    private void onIsInsideWall(CallbackInfoReturnable<Boolean> cir) {
        if (isRecentlyCollidingWithPortal()) {
            cir.setReturnValue(false);
        }
    }
    
    //for teleportation debug
    @Inject(
        method = "setPos",
        at = @At("HEAD")
    )
    private void onSetPos(double nx, double ny, double nz, CallbackInfo ci) {
        if (((Object) this) instanceof ServerPlayerEntity) {
            if (Global.teleportationDebugEnabled) {
                if (Math.abs(getX() - nx) > 10 ||
                    Math.abs(getY() - ny) > 10 ||
                    Math.abs(getZ() - nz) > 10
                ) {
                    Helper.log(String.format(
                        "%s %s teleported from %s %s %s to %s %s %s",
                        getName().asString(),
                        world.getRegistryKey(),
                        (int) getX(), (int) getY(), (int) getZ(),
                        (int) nx, (int) ny, (int) nz
                    ));
                    new Throwable().printStackTrace();
                }
            }
        }
    }
    
    // Avoid instant crouching when crossing a scaling portal
    @Inject(method = "setPose", at = @At("HEAD"), cancellable = true)
    private void onSetPose(EntityPose pose, CallbackInfo ci) {
        Entity this_ = (Entity) (Object) this;
        
        if (this_ instanceof ServerPlayerEntity) {
            if (this_.getPose() == EntityPose.STANDING) {
                if (pose == EntityPose.CROUCHING || pose == EntityPose.SWIMMING) {
                    if (isRecentlyCollidingWithPortal() ||
                        Global.serverTeleportationManager.isJustTeleported(this_, 20)
                    ) {
                        ci.cancel();
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
            if (collidingPortal.world != world) {
                collidingPortal = null;
            }
            else {
                if (!getBoundingBox().expand(0.5).intersects(collidingPortal.getBoundingBox())) {
                    collidingPortal = null;
                }
            }
            
            if (Math.abs(world.getTime() - collidingPortalActiveTickTime) >= 3) {
                collidingPortal = null;
            }
        }
        
        if (world.isClient) {
            McHelper.onClientEntityTick(this_);
        }
    }
    
    @Override
    public void notifyCollidingWithPortal(Entity portal) {
        collidingPortal = portal;
        collidingPortalActiveTickTime = world.getTime();
    }
    
    @Override
    public boolean isRecentlyCollidingWithPortal() {
        return (world.getTime() - collidingPortalActiveTickTime) < 20;
    }
    
    @Override
    public void portal_requestUpdateChunkPos() {
        chunkPosUpdateRequested = true;
    }
}
