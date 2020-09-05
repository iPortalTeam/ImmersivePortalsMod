package com.qouteall.immersive_portals.mixin.client.render;

import com.qouteall.immersive_portals.render.context_management.PortalRendering;
import net.minecraft.client.render.BackgroundRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(BackgroundRenderer.class)
public class MixinBackgroundRenderer_R {
    // avoid thick fog when rendering an upscaling portal
    @ModifyArg(
        method = "applyFog",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/RenderSystem;fogStart(F)V"
        )
    )
    private static float modifyFogStart(float fogStart) {
        return multiplyByPortalScale(fogStart);
    }
    
    @ModifyArg(
        method = "applyFog",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/RenderSystem;fogEnd(F)V"
        )
    )
    private static float modifyFogEnd(float fogStart) {
        return multiplyByPortalScale(fogStart);
    }
    
    private static float multiplyByPortalScale(float value) {
        if (PortalRendering.isRendering()) {
            double scaling = PortalRendering.getRenderingPortal().scaling;
            if (scaling > 1) {
                return ((float) (value * scaling));
            }
        }
        return value;
    }
}
