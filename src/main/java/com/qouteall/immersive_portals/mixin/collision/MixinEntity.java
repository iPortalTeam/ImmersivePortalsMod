package com.qouteall.immersive_portals.mixin.collision;

import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.ducks.IEEntity;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.portal.global_portals.WorldWrappingPortal;
import com.qouteall.immersive_portals.teleportation.CollisionHelper;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class MixinEntity implements IEEntity {
    //world.getEntities is not reliable
    //it has a small chance to ignore collided entities
    //this would cause player to fall through floor when halfway though portal
    //so when player stops colliding a portal, it will not stop colliding instantly
    //it will stop colliding when counter turn to 0
    
    private Portal collidingPortal;
    private int stopCollidingPortalCounter;
    
    @Shadow
    public abstract Box getBoundingBox();
    
    @Shadow
    public World world;
    
    @Shadow
    public abstract void setBoundingBox(Box box_1);
    
    @Shadow
    public DimensionType dimension;
    
    @Shadow
    protected abstract Vec3d adjustMovementForCollisions(Vec3d vec3d_1);
    
    @Shadow
    public abstract Text getName();
    
    @Shadow public abstract double getX();
    
    @Shadow public abstract double getY();
    
    @Shadow public abstract double getZ();
    
    @Shadow protected abstract BlockPos getLandingPos();
    
    @Shadow public boolean inanimate;
    
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
        if (attemptedMove.lengthSquared() > 256) {
            Helper.err("Entity moving too fast " + entity + attemptedMove);
            return Vec3d.ZERO;
        }
        
        if (collidingPortal == null) {
            return adjustMovementForCollisions(attemptedMove);
        }
        
        if (entity.hasPassengers() || entity.hasVehicle()) {
            return adjustMovementForCollisions(attemptedMove);
        }
        
        Vec3d result = CollisionHelper.handleCollisionHalfwayInPortal(
            (Entity) (Object) this,
            attemptedMove,
            collidingPortal,
            attemptedMove1 -> adjustMovementForCollisions(attemptedMove1)
        );
        return result;
    }
    
    //don't burn when jumping into end portal
    //teleportation is instant and accurate in client but not in server
    //so collision may sometimes be incorrect when client teleported but server did not teleport
    @Inject(method = "setInLava", at = @At("HEAD"), cancellable = true)
    private void onSetInLava(CallbackInfo ci) {
        if (CollisionHelper.isNearbyPortal((Entity) (Object) this)) {
            ci.cancel();
        }
    }
    
    @Inject(
        method = "isFireImmune",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onIsFireImmune(CallbackInfoReturnable<Boolean> cir) {
        if (collidingPortal != null) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }
    
    @Inject(
        method = "setOnFireFor",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onSetOnFireFor(int int_1, CallbackInfo ci) {
        if (CollisionHelper.isNearbyPortal((Entity) (Object) this)) {
            ci.cancel();
        }
    }
    
    @Redirect(
        method = "move",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/Entity;isWet()Z"
        )
    )
    private boolean redirectIsWet(Entity entity) {
        if (collidingPortal != null) {
            return true;
        }
        return entity.isWet();
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
    
    @Inject(
        method = "fromTag",
        at = @At("RETURN")
    )
    private void onReadFinished(CompoundTag compound, CallbackInfo ci) {
        if (dimension == null) {
            Helper.err("Invalid Dimension Id Read From NBT " + this);
            if (world != null) {
                dimension = world.getDimension().getType();
            }
            else {
                Helper.err("World Field is Null");
                dimension = DimensionType.OVERWORLD;
            }
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
                        dimension,
                        (int) getX(), (int) getY(), (int) getZ(),
                        (int) nx, (int) ny, (int) nz
                    ));
                    new Throwable().printStackTrace();
                }
            }
        }
    }
    
    //avoid suffocation damage when crossing world wrapping portal with barrier
    @Inject(
        method = "isInsideWall",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onIsInsideWall(CallbackInfoReturnable<Boolean> cir) {
        if (collidingPortal instanceof WorldWrappingPortal) {
            cir.setReturnValue(false);
        }
    }
    
    @Override
    public Portal getCollidingPortal() {
        return collidingPortal;
    }
    
    @Override
    public void tickCollidingPortal(float tickDelta) {
        Entity this_ = (Entity) (Object) this;
        
        if (collidingPortal != null) {
            if (collidingPortal.dimension != dimension) {
                collidingPortal = null;
            }
        }
        
        //TODO change to portals discovering nearby entities instead
        // of entities discovering nearby portals
        world.getProfiler().push("getCollidingPortal");
        Portal nowCollidingPortal =
            CollisionHelper.getCollidingPortalUnreliable(this_, tickDelta);
        world.getProfiler().pop();
        
        if (nowCollidingPortal == null) {
            if (stopCollidingPortalCounter > 0) {
                stopCollidingPortalCounter--;
            }
            else {
                collidingPortal = null;
            }
        }
        else {
            collidingPortal = nowCollidingPortal;
            stopCollidingPortalCounter = 1;
        }
        
        if (world.isClient) {
            McHelper.onClientEntityTick(this_);
        }
    }
}
