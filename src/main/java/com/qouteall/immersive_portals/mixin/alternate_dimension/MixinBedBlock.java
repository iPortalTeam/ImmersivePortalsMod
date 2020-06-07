package com.qouteall.immersive_portals.mixin.alternate_dimension;

import net.minecraft.block.BedBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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
