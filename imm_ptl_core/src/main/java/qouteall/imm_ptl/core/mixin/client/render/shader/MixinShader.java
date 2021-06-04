package qouteall.imm_ptl.core.mixin.client.render.shader;

import qouteall.imm_ptl.core.ducks.IEShader;
import qouteall.imm_ptl.core.render.ShaderCodeTransformation;
import net.minecraft.client.gl.GlShader;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.render.Shader;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.resource.ResourceFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.List;

@Mixin(Shader.class)
public abstract class MixinShader implements IEShader {
    @Shadow
    @Nullable
    public abstract GlUniform getUniform(String name);
    
    @Shadow
    @Final
    private List<GlUniform> uniforms;
    @Shadow
    @Final
    private String name;
    
    @Nullable
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
    
    @Inject(
        method = "loadReferences",
        at = @At("HEAD")
    )
    private void onLoadReferences(CallbackInfo ci) {
        GlShader this_ = (GlShader) (Object) this;
        
        if (ShaderCodeTransformation.shouldAddUniform(name)) {
            uniforms.add(new GlUniform(
                "imm_ptl_ClippingEquation",
                7, 4, this_
            ));
        }
    }
    
    @Nullable
    @Override
    public GlUniform ip_getClippingEquationUniform() {
        return ip_clippingEquation;
    }
}
