package com.qouteall.immersive_portals.mixin;

import com.qouteall.immersive_portals.Globals;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayerEntity.class)
public class MixinAbstractClientPlayerEntity {
//    @Inject(
//        method = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;isSpectator()Z",
//        at = @At("HEAD"),
//        cancellable = true
//    )
//    private void onIsSpectator(CallbackInfoReturnable<Boolean> cir) {
//        if (Globals.portalRenderManager.isRendering()) {
//            cir.setReturnValue(true);
//            cir.cancel();
//        }
//    }
}
