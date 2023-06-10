package qouteall.imm_ptl.core.mixin.common.collision;

import net.minecraft.world.entity.decoration.HangingEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(HangingEntity.class)
public class MixinHangingEntity {
//    @Inject(
//        method = "Lnet/minecraft/world/entity/decoration/HangingEntity;tick()V",
//        at = @At("HEAD")
//    )
//    private void onTick(CallbackInfo ci) {
//        ((IEEntity) this).tickCollidingPortal(1);
//    }
}
