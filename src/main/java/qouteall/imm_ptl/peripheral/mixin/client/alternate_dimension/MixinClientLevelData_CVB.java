package qouteall.imm_ptl.peripheral.mixin.client.alternate_dimension;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import qouteall.imm_ptl.peripheral.alternate_dimension.AlternateDimensions;

@Mixin(ClientLevel.ClientLevelData.class)
public class MixinClientLevelData_CVB {
    @Inject(
        method = "Lnet/minecraft/client/multiplayer/ClientLevel$ClientLevelData;getHorizonHeight(Lnet/minecraft/world/level/LevelHeightAccessor;)D",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onGetSkyDarknessHeight(CallbackInfoReturnable<Double> cir) {
        ClientLevel world = Minecraft.getInstance().level;
        assert world != null;
        boolean isAlternateDimension =
            AlternateDimensions.isAlternateDimension(world);
    
        if (isAlternateDimension) {
            cir.setReturnValue(-10000.0);
        }
    }
}
