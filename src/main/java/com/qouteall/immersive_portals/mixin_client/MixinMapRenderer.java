package com.qouteall.immersive_portals.mixin_client;

import com.qouteall.immersive_portals.render.MyRenderHelper;
import net.minecraft.client.gui.MapRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.map.MapState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MapRenderer.class)
public class MixinMapRenderer {
    @Inject(
        method = "draw",
        at = @At("HEAD")
    )
    private void onBeginDraw(
        MatrixStack matrixStack,
        VertexConsumerProvider vertexConsumerProvider,
        MapState mapState,
        boolean bl,
        int i,
        CallbackInfo ci
    ) {
        if (MyRenderHelper.isRenderingOddNumberOfMirrors()) {
            MyRenderHelper.shouldForceDisableCull = true;
        }
    }
    
    @Inject(
        method = "draw",
        at = @At("RETURN")
    )
    private void onEndDraw(
        MatrixStack matrixStack,
        VertexConsumerProvider vertexConsumerProvider,
        MapState mapState,
        boolean bl,
        int i,
        CallbackInfo ci
    ) {
        if (vertexConsumerProvider instanceof VertexConsumerProvider.Immediate) {
            ((VertexConsumerProvider.Immediate) vertexConsumerProvider).draw();
        }
        if (MyRenderHelper.isRenderingOddNumberOfMirrors()) {
            MyRenderHelper.shouldForceDisableCull = false;
        }
    }
}
