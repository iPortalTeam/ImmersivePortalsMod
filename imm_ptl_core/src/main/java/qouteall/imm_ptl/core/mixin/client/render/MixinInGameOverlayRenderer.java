package qouteall.imm_ptl.core.mixin.client.render;

import qouteall.imm_ptl.core.CGlobal;
import qouteall.imm_ptl.core.ducks.IEEntity;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;
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
        Sprite sprite,
        MatrixStack matrices,
        CallbackInfo ci
    ) {
        if (PortalRendering.isRendering()) {
            ci.cancel();
        }
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null) {
            if (((IEEntity) player).getCollidingPortal() != null) {
                ci.cancel();
            }
        }
        if (CGlobal.clientTeleportationManager.isTeleportingFrequently()) {
            ci.cancel();
        }
    }
}
