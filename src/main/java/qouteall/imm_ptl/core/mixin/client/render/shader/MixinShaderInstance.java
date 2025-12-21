package qouteall.imm_ptl.core.mixin.client.render.shader;

import com.mojang.blaze3d.shaders.Shader;
import com.mojang.blaze3d.shaders.Uniform;
import net.minecraft.client.renderer.ShaderInstance;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.ducks.IEShader;
import qouteall.imm_ptl.core.render.ShaderCodeTransformation;

import java.util.List;

@Mixin(ShaderInstance.class)
public abstract class MixinShaderInstance implements IEShader {
    @Shadow
    @Nullable
    public abstract Uniform getUniform(String name);
    
    @Shadow
    @Final
    private List<Uniform> uniforms;
    @Shadow
    @Final
    private String name;
    
    @Nullable
    private Uniform ip_clippingEquation;
    
    @Inject(
        method = "Lnet/minecraft/client/renderer/ShaderInstance;updateLocations()V",
        at = @At("HEAD")
    )
    private void onLoadReferences(CallbackInfo ci) {
        Shader this_ = (Shader) (Object) this;
        
        if (ShaderCodeTransformation.shouldAddUniform(name)) {
            ip_clippingEquation = new Uniform(
                "iportal_ClippingEquation",
                7, 4, this_
            );
            uniforms.add(ip_clippingEquation);
        }
    }
    
    @Nullable
    @Override
    public Uniform ip_getClippingEquationUniform() {
        return ip_clippingEquation;
    }
}
