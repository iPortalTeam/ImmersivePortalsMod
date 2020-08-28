package com.qouteall.imm_ptl_peripheral.mixin.common.alternate_dimension;

import net.minecraft.block.BedBlock;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BedBlock.class)
public class MixinBedBlock {
//    @Inject(
//        method = "isOverworld",
//        at = @At("HEAD"),
//        cancellable = true
//    )
//    private static void onIsOverworld(World world, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
//        if (world.getDimension() instanceof AlternateDimension) {
//            cir.setReturnValue(true);
//        }
//    }
}
