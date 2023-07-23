package qouteall.imm_ptl.core.mixin.common.collision;

import net.minecraft.world.entity.vehicle.AbstractMinecart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.IPGlobal;

@Mixin(AbstractMinecart.class)
public class MixinAbstractMinecartEntity {
    // for debugging
    @Inject(
        method = "Lnet/minecraft/world/entity/vehicle/AbstractMinecart;lerpTo(DDDFFIZ)V",
        at = @At("RETURN")
    )
    private void onUpdateTracketPositionAndAngles(
        double x, double y, double z, float yaw, float pitch, int interpolationSteps,
        boolean interpolate, CallbackInfo ci
    ) {
        AbstractMinecart this_ = (AbstractMinecart) ((Object) this);
        if (!IPGlobal.allowClientEntityPosInterpolation) {
            this_.setPos(x, y, z);
        }
    }
}
