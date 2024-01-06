package qouteall.imm_ptl.core.mixin.client.interaction;

import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Item.class)
public class MixinItem_Interaction_Client {
//    @Inject(method = "getPlayerPOVHitResult", at = @At("HEAD"), cancellable = true)
//    private static void onGetPlayerPOVHitResult(
//        Level level, Player player, ClipContext.Fluid fluidMode,
//        CallbackInfoReturnable<BlockHitResult> cir
//    ) {
//        if (level.isClientSide()) {
//            HitResult remoteHitResult = BlockManipulationClient.remoteHitResult;
//            if (remoteHitResult instanceof BlockHitResult blockHitResult) {
//                cir.setReturnValue(blockHitResult);
//            }
//        }
//    }
}
