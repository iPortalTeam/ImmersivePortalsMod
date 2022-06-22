package qouteall.imm_ptl.peripheral.mixin.client;

import net.minecraft.client.gui.components.BossHealthOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BossHealthOverlay.class)
public class MixinBossHealthOverlay {
    // the boss info does not get synced through portal
    // so when the player jumps into end portal the fog will abruptly become thick
    // avoid thicken fog to make the teleportation seamless
    @Inject(method = "Lnet/minecraft/client/gui/components/BossHealthOverlay;shouldCreateWorldFog()Z", at = @At("HEAD"), cancellable = true)
    private void onShouldThickenFog(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
        cir.cancel();
    }
}
