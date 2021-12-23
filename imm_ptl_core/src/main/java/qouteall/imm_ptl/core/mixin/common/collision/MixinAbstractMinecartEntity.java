package qouteall.imm_ptl.core.mixin.common.collision;

import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.ducks.IEEntity;

@Mixin(AbstractMinecartEntity.class)
public class MixinAbstractMinecartEntity {
    @Shadow
    private int clientInterpolationSteps;
    
    @Inject(
        method = "tick",
        at = @At("HEAD")
    )
    private void onTick(CallbackInfo ci) {
        ((IEEntity) this).tickCollidingPortal(1);
    }
    
    @Inject(
        method = "updateTrackedPositionAndAngles",
        at = @At("RETURN")
    )
    private void onUpdateTracketPositionAndAngles(
        double x, double y, double z, float yaw, float pitch, int interpolationSteps,
        boolean interpolate, CallbackInfo ci
    ) {
        AbstractMinecartEntity this_ = (AbstractMinecartEntity) ((Object) this);
        if (!IPGlobal.allowClientEntityPosInterpolation) {
            this_.setPosition(x, y, z);
        }
    }
}
