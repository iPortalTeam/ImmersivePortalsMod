package com.qouteall.immersive_portals.mixin_client;

import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.render.FrustumCuller;
import net.minecraft.client.render.Frustum;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Supplier;

@Mixin(Frustum.class)
public class MixinFrustum {
    @Shadow
    private double x;
    @Shadow
    private double y;
    @Shadow
    private double z;
    
    private FrustumCuller frustumCuller;
    
    @Inject(
        method = "setPosition",
        at = @At("TAIL")
    )
    private void onSetOrigin(double double_1, double double_2, double double_3, CallbackInfo ci) {
        if (frustumCuller == null) {
            frustumCuller = new FrustumCuller();
        }
        frustumCuller.update(x, y, z);
    }
    
    @Inject(
        method = "Lnet/minecraft/client/render/Frustum;isVisible(DDDDDD)Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onIntersectionTest(
        double double_1,
        double double_2,
        double double_3,
        double double_4,
        double double_5,
        double double_6,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (CGlobal.doUseAdvancedFrustumCulling) {
            //this allocation should be avoided by jvm
            Supplier<Box> boxInLocalCoordinateSupplier = () -> new Box(
                double_1, double_2, double_3,
                double_4, double_5, double_6
            ).offset(
                -x, -y, -z
            );
    
            if (frustumCuller.canDetermineInvisible(boxInLocalCoordinateSupplier)) {
                cir.setReturnValue(false);
                cir.cancel();
            }
        }
    }
    
    
}
