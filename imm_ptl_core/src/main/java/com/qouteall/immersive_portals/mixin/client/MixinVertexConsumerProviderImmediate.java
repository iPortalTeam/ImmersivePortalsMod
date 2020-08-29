package com.qouteall.immersive_portals.mixin.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.qouteall.immersive_portals.render.context_management.PortalRendering;
import com.qouteall.immersive_portals.render.context_management.RenderStates;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VertexConsumerProvider.Immediate.class)
public class MixinVertexConsumerProviderImmediate {
    @Inject(
        method = "draw(Lnet/minecraft/client/render/RenderLayer;)V",
        at = @At("HEAD")
    )
    private void onBeginDraw(RenderLayer layer, CallbackInfo ci) {
        if (PortalRendering.isRenderingOddNumberOfMirrors()) {
            RenderStates.shouldForceDisableCull = true;
            GlStateManager.disableCull();
        }
    }
    
    @Inject(
        method = "draw(Lnet/minecraft/client/render/RenderLayer;)V",
        at = @At("RETURN")
    )
    private void onEndDraw(RenderLayer layer, CallbackInfo ci) {
        RenderStates.shouldForceDisableCull = false;
        GlStateManager.enableCull();
    }
}
