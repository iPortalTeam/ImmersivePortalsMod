package qouteall.imm_ptl.core.mixin.client;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.ducks.IEEntity;
import qouteall.imm_ptl.core.portal.Portal;

@Mixin(Entity.class)
public class MixinLivingEntity_C {
    // avoid entity position interpolate when crossing portal to the same dimension
    @Inject(
        method = "moveOrInterpolateTo(Lnet/minecraft/world/phys/Vec3;FF)V",
        at = @At("RETURN")
    )
    private void onUpdateTrackedPositionAndAngles(
        Vec3 position,
        float yaw,
        float pitch,
        CallbackInfo ci
    ) {
        Entity this_ = (Entity) (Object) this;
        if (!(this_ instanceof LivingEntity)) {
            return;
        }
        if (!IPGlobal.allowClientEntityPosInterpolation) {
            this_.setPos(position.x, position.y, position.z);
            return;
        }
        
        Portal collidingPortal = ((IEEntity) this).ip_getCollidingPortal();
        if (collidingPortal != null) {
            Vec3 interpolationPos = this_.getInterpolation().position();
            double dx = this_.getX() - interpolationPos.x;
            double dy = this_.getY() - interpolationPos.y;
            double dz = this_.getZ() - interpolationPos.z;
            if (dx * dx + dy * dy + dz * dz > 4) {
                Vec3 currPos = interpolationPos;
                McHelper.setPosAndLastTickPos(
                    this_,
                    currPos,
                    currPos.subtract(McHelper.getWorldVelocity(this_))
                );
                McHelper.updateBoundingBox(this_);
            }
        }
    }
}
