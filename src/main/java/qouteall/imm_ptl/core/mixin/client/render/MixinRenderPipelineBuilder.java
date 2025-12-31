package qouteall.imm_ptl.core.mixin.client.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import qouteall.imm_ptl.core.render.ShaderCodeTransformation;

import java.util.Optional;

@Mixin(value = RenderPipeline.Builder.class, remap = false)
public abstract class MixinRenderPipelineBuilder {
    @Shadow
    private Optional<Identifier> vertexShader;
    
    @Inject(method = "build", at = @At("HEAD"))
    private void ip_addClippingUniform(CallbackInfoReturnable<RenderPipeline> cir) {
        if (vertexShader.isEmpty()) {
            return;
        }
        Identifier shaderId = vertexShader.get();
        if (ShaderCodeTransformation.shouldAddUniform(shaderId)) {
            ((RenderPipeline.Builder) (Object) this)
                .withUniform("IPortalClipping", UniformType.UNIFORM_BUFFER);
        }
    }
}
