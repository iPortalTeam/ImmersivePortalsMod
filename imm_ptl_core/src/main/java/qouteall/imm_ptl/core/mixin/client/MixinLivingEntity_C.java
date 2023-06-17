package qouteall.imm_ptl.core.mixin.client;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.ducks.IEEntity;
import qouteall.imm_ptl.core.portal.Portal;

@Mixin(LivingEntity.class)
public class MixinLivingEntity_C {
    @Shadow
    protected double lerpX;
    
    @Shadow
    protected double lerpY;
    
    @Shadow
    protected double lerpZ;
    
    @Shadow
    protected int lerpSteps;
    
    // avoid entity position interpolate when crossing portal to the same dimension
    @Inject(
        method = "Lnet/minecraft/world/entity/LivingEntity;lerpTo(DDDFFIZ)V",
        at = @At("RETURN")
    )
    private void onUpdateTrackedPositionAndAngles(
        double x,
        double y,
        double z,
        float yaw,
        float pitch,
        int interpolationSteps,
        boolean interpolate,
        CallbackInfo ci
    ) {
        LivingEntity this_ = ((LivingEntity) (Object) this);
        if (!IPGlobal.allowClientEntityPosInterpolation) {
            this_.setPos(x, y, z);
            return;
        }
        
        Portal collidingPortal = ((IEEntity) this).ip_getCollidingPortal();
        if (collidingPortal != null) {
            
            double dx = this_.getX() - lerpX;
            double dy = this_.getY() - lerpY;
            double dz = this_.getZ() - lerpZ;
            if (dx * dx + dy * dy + dz * dz > 4) {
                Vec3 currPos = new Vec3(lerpX, lerpY, lerpZ);
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
