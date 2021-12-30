package qouteall.imm_ptl.core.compat;

import me.andrew.gravitychanger.accessor.EntityAccessor;
import me.andrew.gravitychanger.accessor.RotatableEntityAccessor;
import me.andrew.gravitychanger.api.GravityChangerAPI;
import me.andrew.gravitychanger.util.RotationUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import qouteall.imm_ptl.core.CHelper;
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
            if (entity instanceof PlayerEntity && entity.world.isClient()) {
                warnGravityChangerNotPresent();
            }
        }
        
        @Nullable
        public DQuaternion getExtraCameraRotation(Direction gravityDirection) {
            return null;
        }
        
        public Vec3d getWorldVelocity(Entity entity) {
            return entity.getVelocity();
        }
        
        public void setWorldVelocity(Entity entity, Vec3d newVelocity) {
            entity.setVelocity(newVelocity);
        }
    }
    
    private static boolean warned = false;
    
    @Environment(EnvType.CLIENT)
    private static void warnGravityChangerNotPresent() {
        if (!warned) {
            warned = true;
            CHelper.printChat(new TranslatableText("imm_ptl.missing_gravity_changer"));
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
                return GravityChangerAPI.getEyeOffset(player);
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
        
        @Override
        public Vec3d getWorldVelocity(Entity entity) {
            if (entity instanceof PlayerEntity player) {
                return GravityChangerAPI.getWorldVelocity(player);
            }
            else {
                return super.getWorldVelocity(entity);
            }
        }
        
        @Override
        public void setWorldVelocity(Entity entity, Vec3d newVelocity) {
            if (entity instanceof PlayerEntity player) {
                GravityChangerAPI.setWorldVelocity(player, newVelocity);
            }
            else {
                super.setWorldVelocity(entity, newVelocity);
            }
        }
    }
}
