package qouteall.imm_ptl.core.mixin.common.collision;

import net.minecraft.world.entity.decoration.HangingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.ducks.IEEntity;

@Mixin(HangingEntity.class)
public class MixinAbstractDecorationEntity {
    @Inject(
        method = "Lnet/minecraft/world/entity/decoration/HangingEntity;tick()V",
        at = @At("HEAD")
    )
    private void onTick(CallbackInfo ci) {
        ((IEEntity) this).tickCollidingPortal(1);
    }
}
