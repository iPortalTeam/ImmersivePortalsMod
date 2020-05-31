package com.qouteall.immersive_portals.mixin_client;

import com.qouteall.immersive_portals.ducks.IEEntity;
import com.qouteall.immersive_portals.render.context_management.PortalLayers;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameOverlayRenderer.class)
public class MixinInGameOverlayRenderer {
    //avoid rendering suffocating when colliding with portal
    @Inject(
        method = "renderInWallOverlay",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void onRenderInWallOverlay(
        MinecraftClient minecraftClient,
        Sprite sprite,
        MatrixStack matrixStack,
        CallbackInfo ci
    ) {
        if (PortalLayers.isRendering()) {
            ci.cancel();
        }
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null) {
            if (((IEEntity) player).getCollidingPortal() != null) {
                ci.cancel();
            }
        }
    }
}
