package qouteall.imm_ptl.peripheral.mixin.client.alternate_dimension;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import qouteall.imm_ptl.peripheral.alternate_dimension.AlternateDimensions;

@Mixin(ClientWorld.Properties.class)
public class MixinClientWorldProperties {
    @Inject(
        method = "getSkyDarknessHeight",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onGetSkyDarknessHeight(CallbackInfoReturnable<Double> cir) {
        boolean isAlternateDimension =
            AlternateDimensions.isAlternateDimension(MinecraftClient.getInstance().world);
    
        if (isAlternateDimension) {
            cir.setReturnValue(-10000.0);
        }
    }
}
