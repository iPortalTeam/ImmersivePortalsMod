package com.qouteall.immersive_portals.mixin_client;

import com.qouteall.immersive_portals.ducks.IEEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerEntity.class)
public class MixinClientPlayerEntity {
    @Inject(
        method = "cannotFitAt",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onCannotFitAt(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (((IEEntity) this).getCollidingPortal() != null) {
            cir.setReturnValue(false);
        }
    }
    
    //use portal culled collision box
//    @Redirect(
//        method = "cannotFitAt",
//        at = @At(
//            value = "INVOKE",
//            target = "Lnet/minecraft/client/network/ClientPlayerEntity;getBoundingBox()Lnet/minecraft/util/math/Box;"
//        )
//    )
//    private Box redirectGetBoundingBox(ClientPlayerEntity clientPlayerEntity) {
//        return CollisionHelper.getActiveCollisionBox(clientPlayerEntity);
//    }
}
