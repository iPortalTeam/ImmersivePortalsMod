package qouteall.imm_ptl.core.compat;

import com.fusionflux.gravity_api.api.GravityChangerAPI;
import com.fusionflux.gravity_api.api.RotationParameters;
import com.fusionflux.gravity_api.util.GravityComponent;
import com.fusionflux.gravity_api.util.RotationUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.Validate;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.McHelper;
import qouteall.q_misc_util.my_util.DQuaternion;

import javax.annotation.Nullable;

public class GravityChangerInterface {
    public static Invoker invoker = new Invoker();
    
    public static class Invoker {
        public boolean isGravityChangerPresent() {
            return false;
        }
        
        public Vec3 getEyeOffset(Entity entity) {
            return new Vec3(0, entity.getEyeHeight(), 0);
        }
        
        public Direction getGravityDirection(Entity entity) {
            return Direction.DOWN;
        }
        
        public void setClientPlayerGravityDirection(Player player, Direction direction) {
            warnGravityChangerNotPresent();
        }
        
        public void setGravityDirectionServer(Entity entity, Direction direction) {
            // nothing
        }
        
        @Nullable
        public DQuaternion getExtraCameraRotation(Direction gravityDirection) {
            return null;
        }
        
        public Vec3 getWorldVelocity(Entity entity) {
            return entity.getDeltaMovement();
        }
        
        public void setWorldVelocity(Entity entity, Vec3 newVelocity) {
            entity.setDeltaMovement(newVelocity);
        }
        
        public Vec3 transformPlayerToWorld(Direction gravity, Vec3 vec3d) {
            return vec3d;
        }
        
        public Vec3 transformWorldToPlayer(Direction gravity, Vec3 vec3d) {
            return vec3d;
        }
        
        public Direction transformDirPlayerToWorld(Direction gravity, Direction direction) {
            return direction;
        }
        
        public Direction transformDirWorldToPlayer(Direction gravity, Direction direction) {
            return direction;
        }
    }
    
    private static boolean warned = false;
    
    @Environment(EnvType.CLIENT)
    private static void warnGravityChangerNotPresent() {
        if (!warned) {
            warned = true;
            CHelper.printChat(Component.translatable("imm_ptl.missing_gravity_changer")
                .append(McHelper.getLinkText("https://github.com/qouteall/Gravity-Api/releases"))
            );
        }
    }
    
    public static class OnGravityChangerPresent extends Invoker {
        
        
        public OnGravityChangerPresent() {
        }
        
        @Override
        public boolean isGravityChangerPresent() {
            return true;
        }
        
        @Override
        public Vec3 getEyeOffset(Entity entity) {
            if (entity instanceof Player player) {
                return GravityChangerAPI.getEyeOffset(player);
            }
            else {
                return super.getEyeOffset(entity);
            }
        }
        
        @Override
        public Direction getGravityDirection(Entity entity) {
            return GravityChangerAPI.getGravityDirection(entity);
        }
        
        @Override
        public void setGravityDirectionServer(Entity entity, Direction direction) {
            GravityChangerAPI.setDefaultGravityDirection(
                entity,
                direction,
                (new RotationParameters()).rotationTime(0)
            );
        }
        
        @Override
        public void setClientPlayerGravityDirection(Player player, Direction direction) {
            setClientPlayerGravityDirectionClientOnly(player, direction);
        }
        
        @Environment(EnvType.CLIENT)
        private void setClientPlayerGravityDirectionClientOnly(
            Player player, Direction direction
        ) {
            Validate.isTrue(Minecraft.getInstance().isSameThread());
            
            GravityComponent gravityComponent = GravityChangerAPI.getGravityComponent(player);
            gravityComponent.setDefaultGravityDirection(
                direction,
                (new RotationParameters()).rotationTime(0),
                false // not initial gravity
            );
            
            // it does not use GravityChangerAPI.setDefaultGravityDirectionClient
            // because immptl has its own verification logic
            // see ServerTeleportationManager
        }
        
        @Nullable
        @Override
        public DQuaternion getExtraCameraRotation(Direction gravityDirection) {
            if (gravityDirection == Direction.DOWN) {
                return null;
            }
            
            return DQuaternion.fromMcQuaternion(RotationUtil.getWorldRotationQuaternion(gravityDirection));
        }
        
        @Override
        public Vec3 getWorldVelocity(Entity entity) {
            if (entity instanceof Player player) {
                return GravityChangerAPI.getWorldVelocity(player);
            }
            else {
                return super.getWorldVelocity(entity);
            }
        }
        
        @Override
        public void setWorldVelocity(Entity entity, Vec3 newVelocity) {
            if (entity instanceof Player player) {
                GravityChangerAPI.setWorldVelocity(player, newVelocity);
            }
            else {
                super.setWorldVelocity(entity, newVelocity);
            }
        }
        
        @Override
        public Vec3 transformPlayerToWorld(Direction gravity, Vec3 vec3d) {
            return RotationUtil.vecPlayerToWorld(vec3d, gravity);
        }
        
        @Override
        public Vec3 transformWorldToPlayer(Direction gravity, Vec3 vec3d) {
            return RotationUtil.vecWorldToPlayer(vec3d, gravity);
        }
        
        @Override
        public Direction transformDirPlayerToWorld(Direction gravity, Direction direction) {
            return RotationUtil.dirPlayerToWorld(direction, gravity);
        }
        
        @Override
        public Direction transformDirWorldToPlayer(Direction gravity, Direction direction) {
            return RotationUtil.dirWorldToPlayer(direction, gravity);
        }
    }
}
