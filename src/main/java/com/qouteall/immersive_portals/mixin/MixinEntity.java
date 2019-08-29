package com.qouteall.immersive_portals.mixin;

import com.qouteall.immersive_portals.CollisionHelper;
import com.qouteall.immersive_portals.portal.Portal;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Entity.class)
public abstract class MixinEntity {
    @Shadow
    public abstract Box getBoundingBox();
    
    @Shadow
    public World world;
    
    @Shadow
    protected abstract Vec3d handleCollisions(Vec3d vec3d_1);
    
    @Shadow
    public abstract void setBoundingBox(Box box_1);
    
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
        
        Portal collidingPortal = CollisionHelper.getCollidingPortal(entity, 0.3);
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
    
}
