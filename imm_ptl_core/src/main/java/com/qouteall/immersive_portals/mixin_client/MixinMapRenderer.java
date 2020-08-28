package com.qouteall.immersive_portals.mixin_client;

import net.minecraft.client.gui.MapRenderer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(MapRenderer.class)
public class MixinMapRenderer {
//    @Inject(
//        method = "draw",
//        at = @At("HEAD")
//    )
//    private void onBeginDraw(
//        MatrixStack matrixStack,
//        VertexConsumerProvider vertexConsumerProvider,
//        MapState mapState,
//        boolean bl,
//        int i,
//        CallbackInfo ci
//    ) {
//        if (PortalRendering.isRenderingOddNumberOfMirrors()) {
//            RenderStates.shouldForceDisableCull = true;
//        }
//    }
//
//    @Inject(
//        method = "draw",
//        at = @At("RETURN")
//    )
//    private void onEndDraw(
//        MatrixStack matrixStack,
//        VertexConsumerProvider vertexConsumerProvider,
//        MapState mapState,
//        boolean bl,
//        int i,
//        CallbackInfo ci
//    ) {
//        if (vertexConsumerProvider instanceof VertexConsumerProvider.Immediate) {
//            ((VertexConsumerProvider.Immediate) vertexConsumerProvider).draw();
//        }
//        if (PortalRendering.isRenderingOddNumberOfMirrors()) {
//            RenderStates.shouldForceDisableCull = false;
//        }
//    }
}
