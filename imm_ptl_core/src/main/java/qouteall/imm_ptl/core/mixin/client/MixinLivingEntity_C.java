package qouteall.imm_ptl.core.mixin.client;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
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
    protected double serverX;
    
    @Shadow
    protected double serverY;
    
    @Shadow
    protected double serverZ;
    
    @Shadow
    protected int bodyTrackingIncrements;
    
    //avoid entity position interpolate when crossing portal when not travelling dimension
    @Inject(
        method = "updateTrackedPositionAndAngles",
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
            this_.setPosition(x, y, z);
            return;
        }
        
        Portal collidingPortal = ((IEEntity) this).getCollidingPortal();
        if (collidingPortal != null) {
            
            double dx = this_.getX() - serverX;
            double dy = this_.getY() - serverY;
            double dz = this_.getZ() - serverZ;
            if (dx * dx + dy * dy + dz * dz > 4) {
                Vec3d currPos = new Vec3d(serverX, serverY, serverZ);
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
