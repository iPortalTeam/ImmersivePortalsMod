package com.qouteall.immersive_portals.mixin;

import net.minecraft.client.render.entity.EntityRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(EntityRenderDispatcher.class)
public class MixinEntityRenderDispatcher {
//    @Inject(
//        method = "Lnet/minecraft/client/render/entity/EntityRenderDispatcher;shouldRender(Lnet/minecraft/entity/Entity;Lnet/minecraft/client/render/VisibleRegion;DDD)Z",
//        at = @At("HEAD"),
//        cancellable = true
//    )
//    private void onShouldRenderEntity(
//        Entity entity_1,
//        VisibleRegion visibleRegion_1,
//        double double_1,
//        double double_2,
//        double double_3,
//        CallbackInfoReturnable<Boolean> cir
//    ) {
//        if (!Globals.portalRenderManager.shouldRenderEntityNow(entity_1)) {
//            cir.setReturnValue(false);
//            cir.cancel();
//        }
//    }
}
