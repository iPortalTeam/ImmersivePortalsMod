package com.qouteall.immersive_portals.mixin.client.render.shader_trans;

import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.render.Shader;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.resource.ResourceFactory;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Shader.class)
public abstract class MixinShader {
    @Shadow
    @Nullable
    public abstract GlUniform getUniform(String name);
    
    private GlUniform ip_clippingEquation;
    
    @Inject(
        method = "<init>",
        at = @At("RETURN")
    )
    private void onConstructed(
        ResourceFactory factory, String name,
        VertexFormat format, CallbackInfo ci
    ) {
        ip_clippingEquation = getUniform("imm_ptl_ClippingEquation");
    }
}
