package com.qouteall.immersive_portals.mixin_client.altius_world;

import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.portal.global_portals.VerticalConnectingPortal;
import com.qouteall.immersive_portals.render.MyGameRenderer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(WorldRenderer.class)
public class MixinWorldRenderer_A {
    @Redirect(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/WorldRenderer;renderSky(Lnet/minecraft/client/util/math/MatrixStack;F)V"
        )
    )
    private void redirectRenderSky(WorldRenderer worldRenderer, MatrixStack matrixStack, float f) {
        if (CGlobal.renderer.isRendering()) {
            if (CGlobal.renderer.getRenderingPortal() instanceof VerticalConnectingPortal) {
                MyGameRenderer.renderSkyFor(
                    DimensionType.OVERWORLD,
                    matrixStack, f
                );
                return;
            }
        }
        worldRenderer.renderSky(matrixStack, f);
    }
}
