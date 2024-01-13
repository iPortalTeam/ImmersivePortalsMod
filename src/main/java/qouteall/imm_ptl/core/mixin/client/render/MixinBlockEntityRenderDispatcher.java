package qouteall.imm_ptl.core.mixin.client.render;

import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BlockEntityRenderDispatcher.class)
public class MixinBlockEntityRenderDispatcher {
//    @Inject(
//        method = "Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderDispatcher;render(Lnet/minecraft/world/level/block/entity/BlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V",
//        at = @At("HEAD"),
//        cancellable = true
//    )
//    private <E extends BlockEntity> void onRenderBlockEntity(
//        E blockEntity,
//        float tickDelta,
//        PoseStack matrix,
//        MultiBufferSource vertexConsumerProvider,
//        CallbackInfo ci
//    ) {
//        if (IrisInterface.invoker.isRenderingShadowMap()) {
//            return;
//        }
//        if (PortalRendering.isRendering()) {
//            PortalLike renderingPortal = PortalRendering.getRenderingPortal();
//            if (renderingPortal instanceof Portal portal) {
//                boolean canRender = portal.getPortalShape()
//                    .shouldRenderInside(portal, new AABB(blockEntity.getBlockPos()));
//                if (!canRender) {
//                    ci.cancel();
//                }
//            }
//        }
//    }
}
