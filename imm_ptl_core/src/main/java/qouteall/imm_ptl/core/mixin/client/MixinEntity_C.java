package qouteall.imm_ptl.core.mixin.client;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Entity.class)
public class MixinEntity_C {
    // avoid invisible armor stands to be visible through portal
//    @Redirect(
//        method = "isInvisibleTo",
//        at = @At(
//            value = "INVOKE",
//            target = "Lnet/minecraft/entity/player/PlayerEntity;isSpectator()Z"
//        )
//    )
//    private boolean redirectIsSpectator(PlayerEntity playerEntity) {
//        if (WorldRenderInfo.isRendering()) {
//            return false;
//        }
//        return playerEntity.isSpectator();
//    }
}
