package qouteall.imm_ptl.core.mixin.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.Shader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.render.CrossPortalEntityRenderer;
import qouteall.imm_ptl.core.render.FrontClipping;

import java.util.function.Supplier;

@Mixin(value = RenderSystem.class, remap = false)
public class MixinRenderSystem_Clipping {
    @Inject(
        method = "setShader",
        at = @At("RETURN")
    )
    private static void onSetShader(Supplier<Shader> supplier, CallbackInfo ci) {
        if (CrossPortalEntityRenderer.isRenderingEntityNormally || CrossPortalEntityRenderer.isRenderingEntityProjection) {
            if (FrontClipping.isClippingEnabled) {
                FrontClipping.updateClippingEquationUniformForCurrentShader(true);
            }
        }
    }
}
