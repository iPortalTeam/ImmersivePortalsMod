package qouteall.imm_ptl.core.mixin.common.collision;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import qouteall.imm_ptl.core.portal.PortalPlaceholderBlock;

@Mixin(AbstractArrow.class)
public class MixinAbstractArrow {
    @WrapWithCondition(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/projectile/AbstractArrow;onHit(Lnet/minecraft/world/phys/HitResult;)V"
        )
    )
    private boolean onOnHitWrap(AbstractArrow abstractArrow, HitResult hitResult) {
        return !PortalPlaceholderBlock.isHitOnPlaceholder(hitResult, abstractArrow.level());
    }
}
