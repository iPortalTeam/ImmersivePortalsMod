package qouteall.imm_ptl.core.mixin.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.compat.iris_compatibility.IrisInterface;
import qouteall.imm_ptl.core.portal.PortalLike;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;
import qouteall.q_misc_util.my_util.Plane;

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
//            Plane innerClipping = renderingPortal.getInnerClipping();
//
//            if (innerClipping != null) {
//                AABB box = new AABB(blockEntity.getBlockPos());
//
//                double furthestX = innerClipping.normal().x > 0 ? box.maxX : box.minX;
//                double furthestY = innerClipping.normal().y > 0 ? box.maxY : box.minY;
//                double furthestZ = innerClipping.normal().z > 0 ? box.maxZ : box.minZ;
//
//                boolean canRender = innerClipping.isPointOnPositiveSide(
//                    new Vec3(furthestX, furthestY, furthestZ)
//                );
//
//                if (!canRender) {
//                    ci.cancel();
//                }
//            }
//
//
//        }
//    }
}
