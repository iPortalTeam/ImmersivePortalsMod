package com.qouteall.immersive_portals.mixin.portal_generation;

import net.minecraft.block.NetherPortalBlock;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(NetherPortalBlock.class)
public class MixinNetherPortalBlock {
    
//    @Inject(
//        method = "createPortalAt",
//        at = @At("HEAD"),
//        cancellable = true
//    )
//    private static void onCreatePortal(
//        WorldAccess world,
//        BlockPos pos,
//        CallbackInfoReturnable<Boolean> cir
//    ) {
//        if (world instanceof ServerWorld) {
//            boolean isNearObsidian = Arrays.stream(Direction.values())
//                .anyMatch(direction -> O_O.isObsidian(world.getBlockState(pos.offset(direction))));
//
//            if (!isNearObsidian) {
//                cir.setReturnValue(false);
//                cir.cancel();
//                return;
//            }
//
//            boolean result = NetherPortalGeneration.onFireLitOnObsidian(
//                ((ServerWorld) world),
//                pos
//            );
//
//            cir.setReturnValue(result);
//            cir.cancel();
//        }
//
//
//    }
}
