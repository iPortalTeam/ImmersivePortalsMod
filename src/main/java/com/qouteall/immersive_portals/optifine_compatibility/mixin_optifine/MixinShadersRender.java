package com.qouteall.immersive_portals.optifine_compatibility.mixin_optifine;

import com.qouteall.immersive_portals.render.MyRenderHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.optifine.shaders.ShadersRender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ShadersRender.class, remap = false)
public class MixinShadersRender {
    
    @Inject(
        method = "updateActiveRenderInfo",
        at = @At("RETURN")
    )
    private static void onUpdateCameraForRenderingShadow(
        Camera camera,
        MinecraftClient mc,
        float partialTicks,
        CallbackInfo ci
    ) {
        MyRenderHelper.adjustCameraPos(camera);
    }
}
