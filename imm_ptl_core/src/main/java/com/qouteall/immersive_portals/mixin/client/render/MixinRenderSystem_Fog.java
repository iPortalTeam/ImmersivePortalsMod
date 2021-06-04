package com.qouteall.immersive_portals.mixin.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.qouteall.immersive_portals.render.MyRenderHelper;
import com.qouteall.immersive_portals.render.context_management.RenderStates;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderSystem.class)
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
