package com.qouteall.immersive_portals.mixin;

import net.minecraft.block.FireBlock;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(FireBlock.class)
public class MixinFireBlock {
//    @Inject(
//        method = "Lnet/minecraft/block/FireBlock;onBlockAdded(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Z)V",
//        at = @At("TAIL")
//    )
//    private void onFireAdded(
//        BlockState blockState_1,
//        World world_1,
//        BlockPos blockPos_1,
//        BlockState blockState_2,
//        boolean boolean_1,
//        CallbackInfo ci
//    ) {
//        if (blockState_2.getBlock() != blockState_1.getBlock()) {
//            if (!world_1.isClient) {
//                //world_1.setBlockState(blockPos_1, Blocks.AIR.getDefaultState());
//                NetherPortalGenerator.onFireLit(
//                    ((ServerWorld) world_1),
//                    blockPos_1
//                );
//            }
//        }
//    }
}
