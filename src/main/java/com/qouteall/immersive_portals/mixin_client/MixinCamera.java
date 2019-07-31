package com.qouteall.immersive_portals.mixin_client;

import net.minecraft.client.render.Camera;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public class MixinCamera {
    @Inject(
        method = "getSubmergedFluidState",
        at = @At("HEAD"),
        cancellable = true
    )
    public void getSubmergedFluidState(CallbackInfoReturnable<FluidState> cir) {
        cir.setReturnValue(Fluids.EMPTY.getDefaultState());
        cir.cancel();
    }
}
