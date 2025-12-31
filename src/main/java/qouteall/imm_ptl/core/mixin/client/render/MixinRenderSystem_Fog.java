package qouteall.imm_ptl.core.mixin.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import qouteall.imm_ptl.core.render.MyRenderHelper;

@Mixin(value = RenderSystem.class, remap = false)
public class MixinRenderSystem_Fog {
    @ModifyVariable(
        method = "setShaderFogStart", at = @At("HEAD"), argsOnly = true, require = 0
    )
    private static float onSetShaderFogStart(float f) {
        return MyRenderHelper.transformFogDistance(f);
    }
    
    @ModifyVariable(
        method = "setShaderFogEnd", at = @At("HEAD"), argsOnly = true, require = 0
    )
    private static float onSetShaderFogEnd(float f) {
        return MyRenderHelper.transformFogDistance(f);
    }
}
