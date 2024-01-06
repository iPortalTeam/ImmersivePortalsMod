package qouteall.imm_ptl.core.mixin.common.interaction;

import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Item.class)
public class MixinItem_Interaction {
//    @Redirect(
//        method = "getPlayerPOVHitResult",
//        at = @At(
//            value = "INVOKE",
//            target = "Lnet/minecraft/world/level/Level;clip(Lnet/minecraft/world/level/ClipContext;)Lnet/minecraft/world/phys/BlockHitResult;"
//        )
//    )
//    private static BlockHitResult onGetPlayerPOVHitResult(
//        Level instance, ClipContext clipContext,
//        @Local Player player
//    ) {
//        Vec3 delta = clipContext.getTo().subtract(clipContext.getFrom());
//        Vec3 direction = delta.normalize();
//        PortalUtils.PortalAwareRaytraceResult rtResult = PortalUtils.portalAwareRayTraceFull(
//            instance, clipContext.getFrom(),
//            direction, delta.length(),
//            player,
//            ((IEClipContext) clipContext).ip_getBlock(),
//            ((IEClipContext) clipContext).ip_getFluid(),
//            new ArrayList<>(),
//            1
//        );
//
//        if (rtResult != null) {
//            return rtResult.hitResult();
//        }
//
//        return BlockHitResult.miss(
//            clipContext.getTo(),
//            Direction.getNearest(direction.x, direction.y, direction.z),
//            BlockPos.containing(clipContext.getTo())
//        );
//    }
//
////    @Inject(method = "getPlayerPOVHitResult", at = @At("HEAD"), cancellable = true)
////    private static void onGetPlayerPOVHitResult(
////        Level level, Player player, ClipContext.Fluid fluidMode,
////        CallbackInfoReturnable<BlockHitResult> cir
////    ) {
////        BlockManipulationServer.Context context =
////            BlockManipulationServer.REDIRECT_CONTEXT.get();
////        if (context != null && context.blockHitResult() != null) {
////            cir.setReturnValue(context.blockHitResult());
////        }
////    }
}
