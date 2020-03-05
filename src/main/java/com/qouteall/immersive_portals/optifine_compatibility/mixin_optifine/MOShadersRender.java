package com.qouteall.immersive_portals.optifine_compatibility.mixin_optifine;

import com.qouteall.immersive_portals.optifine_compatibility.OFGlobal;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.optifine.shaders.ShadersRender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ShadersRender.class, remap = false)
public class MOShadersRender {
    @Inject(method = "renderShadowMap", at = @At("HEAD"), cancellable = true)
    private static void onRenderShadowMap(
        GameRenderer entityRenderer,
        Camera activeRenderInfo,
        int pass,
        float partialTicks,
        long finishTimeNano,
        CallbackInfo ci
    ) {
        if (!OFGlobal.alwaysRenderShadowMap) {
            if (OFGlobal.shaderContextManager.isCurrentDimensionRendered()) {
                ci.cancel();
            }
        }
    }
}
