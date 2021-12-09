package qouteall.imm_ptl.core.mixin.client.render;

import qouteall.imm_ptl.core.compat.iris_compatibility.IrisInterface;
import qouteall.imm_ptl.core.portal.PortalLike;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntityRenderDispatcher.class)
public class MixinBlockEntityRenderDispatcher {
    @Inject(
        method = "Lnet/minecraft/client/render/block/entity/BlockEntityRenderDispatcher;render(Lnet/minecraft/block/entity/BlockEntity;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private <E extends BlockEntity> void onRenderBlockEntity(
        E blockEntity,
        float tickDelta,
        MatrixStack matrix,
        VertexConsumerProvider vertexConsumerProvider,
        CallbackInfo ci
    ) {
        if (IrisInterface.invoker.isRenderingShadowMap()) {
            return;
        }
        if (PortalRendering.isRendering()) {
            PortalLike renderingPortal = PortalRendering.getRenderingPortal();
            boolean canRender = renderingPortal.isInside(
                Vec3d.ofCenter(blockEntity.getPos()),
                -0.1
            );
            if (!canRender) {
                ci.cancel();
            }
        }
    }
}
