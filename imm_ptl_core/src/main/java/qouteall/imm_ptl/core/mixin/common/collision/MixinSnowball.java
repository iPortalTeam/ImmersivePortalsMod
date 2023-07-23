package qouteall.imm_ptl.core.mixin.common.collision;

import net.minecraft.world.entity.projectile.Snowball;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Snowball.class)
public abstract class MixinSnowball extends MixinEntity {
//    @Shadow
//    public abstract void onHit(HitResult hitResult);
//
//    @Inject(method = "Lnet/minecraft/world/entity/projectile/Snowball;onHit(Lnet/minecraft/world/phys/HitResult;)V", at = @At(value = "HEAD"), cancellable = true)
//    protected void onHit(HitResult hitResult, CallbackInfo ci) {
//        Entity this_ = (Entity) (Object) this;
//        if (hitResult instanceof BlockHitResult) {
//            Block hittingBlock = this_.level().getBlockState(((BlockHitResult) hitResult).getBlockPos()).getBlock();
//            if (hitResult.getType() == HitResult.Type.BLOCK &&
//                hittingBlock == PortalPlaceholderBlock.instance
//            ) {
//                ci.cancel();
//            }
//        }
//    }
}
