package qouteall.imm_ptl.core.mixin.common.collision;

import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(AbstractArrow.class)
public class MixinAbstractArrow {
    // TODO check arrow go through nether portal
//    @WrapWithCondition(
//        method = "tick",
//        at = @At(
//            value = "INVOKE",
//            target = "Lnet/minecraft/world/entity/projectile/AbstractArrow;onHit(Lnet/minecraft/world/phys/HitResult;)V"
//        )
//    )
//    private boolean onOnHitWrap(AbstractArrow abstractArrow, HitResult hitResult) {
//        return !PortalPlaceholderBlock.isHitOnPlaceholder(hitResult, abstractArrow.level());
//    }
}
