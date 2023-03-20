package qouteall.imm_ptl.core.mixin.common.collision;

import net.minecraft.world.entity.vehicle.AbstractMinecart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.ducks.IEEntity;

@Mixin(AbstractMinecart.class)
public class MixinAbstractMinecartEntity {
    @Shadow
    private int lSteps;
    
//    @Inject(
//        method = "Lnet/minecraft/world/entity/vehicle/AbstractMinecart;tick()V",
//        at = @At("HEAD")
//    )
//    private void onTick(CallbackInfo ci) {
//        ((IEEntity) this).tickCollidingPortal(1);
//    }
    
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
