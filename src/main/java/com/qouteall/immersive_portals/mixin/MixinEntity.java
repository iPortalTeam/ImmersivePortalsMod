package com.qouteall.immersive_portals.mixin;

import com.qouteall.immersive_portals.CollisionHelper;
import com.qouteall.immersive_portals.exposer.IEEntity;
import com.qouteall.immersive_portals.portal.Portal;
import net.minecraft.entity.Entity;
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
    protected abstract Vec3d handleCollisions(Vec3d vec3d_1);
    
    @Shadow
    public abstract void setBoundingBox(Box box_1);
    
    @Shadow
    protected abstract void burn(int int_1);
    
    @Shadow
    public DimensionType dimension;
    
    //maintain collidingPortal field
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTicking(CallbackInfo ci) {
        if (collidingPortal != null) {
            if (collidingPortal.dimension != dimension) {
                collidingPortal = null;
            }
        }
        
        Portal nowCollidingPortal =
            CollisionHelper.getCollidingPortalUnreliable((Entity) (Object) this);
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
    }
    
    @Redirect(
        method = "move",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/Entity;handleCollisions(Lnet/minecraft/util/math/Vec3d;)Lnet/minecraft/util/math/Vec3d;"
        )
    )
    private Vec3d redirectHandleCollisions(Entity entity, Vec3d attemptedMove) {
        if (attemptedMove.lengthSquared() > 16) {
            return handleCollisions(attemptedMove);
        }
        
        if (collidingPortal == null) {
            return handleCollisions(attemptedMove);
        }
        
        Vec3d result = CollisionHelper.handleCollisionHalfwayInPortal(
            (Entity) (Object) this,
            attemptedMove,
            collidingPortal,
            attemptedMove1 -> handleCollisions(attemptedMove1)
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
    
    //don't burn when jumping into end portal
    @Redirect(
        method = "move",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/Entity;burn(I)V"
        )
    )
    private void redirectBurn(Entity entity, int int_1) {
        if (!CollisionHelper.isNearbyPortal((Entity) (Object) this)) {
            burn(int_1);
        }
    }
    
    //don't burn when jumping into end portal
    @Redirect(
        method = "move",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/World;doesAreaContainFireSource(Lnet/minecraft/util/math/Box;)Z"
        )
    )
    private boolean redirectDoesContainFireSource(World world, Box box_1) {
        if (!CollisionHelper.isNearbyPortal((Entity) (Object) this)) {
            return world.doesAreaContainFireSource(box_1);
        }
        else {
            return false;
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
    
    @Override
    public Portal getCollidingPortal() {
        return collidingPortal;
    }
}
