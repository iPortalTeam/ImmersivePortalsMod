package qouteall.imm_ptl.peripheral.mixin.common.alternate_dimension;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import qouteall.imm_ptl.peripheral.alternate_dimension.AlternateDimensions;

@Mixin(ServerLevel.class)
public class MixinServerLevel_ModifySeed {
    @Inject(method = "getSeed", at = @At("RETURN"), cancellable = true)
    private void onGetSeed(CallbackInfoReturnable<Long> cir) {
        Level world = (Level) ((Object) this);
        
        ResourceKey<Level> dimension = world.dimension();
        if (dimension == AlternateDimensions.alternate2) {
            cir.setReturnValue(cir.getReturnValue() + 1);
        }
    }
}
