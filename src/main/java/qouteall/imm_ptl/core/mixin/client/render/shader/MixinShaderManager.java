package qouteall.imm_ptl.core.mixin.client.render.shader;

import com.mojang.blaze3d.shaders.ShaderType;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import qouteall.imm_ptl.core.render.ShaderCodeTransformation;

@Mixin(ShaderManager.class)
public class MixinShaderManager {
    @Inject(method = "getShader", at = @At("RETURN"), cancellable = true)
    private void ip_transformShaderSource(
        Identifier id, ShaderType type, CallbackInfoReturnable<String> cir
    ) {
        String source = cir.getReturnValue();
        if (source == null) {
            return;
        }
        String transformed = ShaderCodeTransformation.transform(type, id, source);
        if (!source.equals(transformed)) {
            cir.setReturnValue(transformed);
        }
    }
}