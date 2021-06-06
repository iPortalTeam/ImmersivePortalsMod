package qouteall.imm_ptl.core.mixin.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import qouteall.imm_ptl.core.render.MyRenderHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = RenderSystem.class, remap = false)
public class MixinRenderSystem_Fog {
    @ModifyVariable(
        method = "_setShaderFogStart", at = @At("HEAD"), argsOnly = true
    )
    private static float onSetShaderFogStart(float f) {
        return MyRenderHelper.transformFogDistance(f);
    }
    
    @ModifyVariable(
        method = "_setShaderFogEnd", at = @At("HEAD"), argsOnly = true
    )
    private static float onSetShaderFogEnd(float f) {
        return MyRenderHelper.transformFogDistance(f);
    }
}
