package qouteall.imm_ptl.core.mixin.client.collisions;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import qouteall.imm_ptl.core.ducks.IEEntity;

@Mixin(LocalPlayer.class)
public class MixinLocalPlayer {
    @Inject(
        method = "Lnet/minecraft/client/player/LocalPlayer;suffocatesAt(Lnet/minecraft/core/BlockPos;)Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onCannotFitAt(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (((IEEntity) this).ip_getCollidingPortal() != null) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }
}
