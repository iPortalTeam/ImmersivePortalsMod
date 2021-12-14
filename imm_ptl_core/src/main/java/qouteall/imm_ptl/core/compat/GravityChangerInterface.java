package qouteall.imm_ptl.core.compat;

import me.andrew.gravitychanger.accessor.EntityAccessor;
import me.andrew.gravitychanger.accessor.RotatableEntityAccessor;
import me.andrew.gravitychanger.util.RotationUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.math.Vec3d;
import qouteall.q_misc_util.my_util.DQuaternion;

import javax.annotation.Nullable;

public class GravityChangerInterface {
    public static Invoker invoker = new Invoker();
    
    public static class Invoker {
        public boolean isGravityChangerPresent() {
            return false;
        }
        
        public Vec3d getEyeOffset(Entity entity) {
            return new Vec3d(0, entity.getStandingEyeHeight(), 0);
        }
        
        public Direction getGravityDirection(PlayerEntity entity) {
            return Direction.DOWN;
        }
        
        public void setGravityDirection(Entity entity, Direction direction) {
        
        }
        
        @Nullable
        public DQuaternion getExtraCameraRotation(Direction gravityDirection) {
            return null;
        }
        
        // temporary workaround
        public void transformVelocityToWorld(Entity entity) {
        
        }
        
        // temporary workaround
        public void transformVelocityToLocal(Entity entity) {
        
        }
    }
    
    public static class OnGravityChangerPresent extends Invoker {
        
        @Override
        public boolean isGravityChangerPresent() {
            return true;
        }
        
        @Override
        public Vec3d getEyeOffset(Entity entity) {
            if (entity instanceof PlayerEntity player) {
                Direction gravityDirection = getGravityDirection(player);
                
                return RotationUtil.vecPlayerToWorld(
                    0.0D, entity.getStandingEyeHeight(), 0.0D, gravityDirection
                );
            }
            else {
                return super.getEyeOffset(entity);
            }
        }
        
        @Override
        public Direction getGravityDirection(PlayerEntity entity) {
            return ((EntityAccessor) entity).gravitychanger$getAppliedGravityDirection();
        }
        
        @Override
        public void setGravityDirection(Entity entity, Direction direction) {
            ((RotatableEntityAccessor) entity).gravitychanger$setGravityDirection(direction, false);
        }
        
        @Nullable
        @Override
        public DQuaternion getExtraCameraRotation(Direction gravityDirection) {
            if (gravityDirection == Direction.DOWN) {
                return null;
            }
            
            return DQuaternion.fromMcQuaternion(
                RotationUtil.getWorldRotationQuaternion(gravityDirection)
            );
        }
    
        // temporary workaround
        @Override
        public void transformVelocityToWorld(Entity entity) {
            if (entity instanceof PlayerEntity player) {
                Direction gravityDirection = getGravityDirection(player);
                Vec3d newVelocity = RotationUtil.vecPlayerToWorld(entity.getVelocity(), gravityDirection);
                entity.setVelocity(newVelocity);
            }
        }
        
        @Override
        public void transformVelocityToLocal(Entity entity) {
            if (entity instanceof PlayerEntity player) {
                Direction gravityDirection = getGravityDirection(player);
                Vec3d newVelocity = RotationUtil.vecWorldToPlayer(entity.getVelocity(), gravityDirection);
                entity.setVelocity(newVelocity);
            }
        }
    }
}
