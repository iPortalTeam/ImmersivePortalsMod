package qouteall.imm_ptl.core.compat.mixin.sodium;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.caffeinemc.mods.sodium.client.gl.shader.ShaderLoader;
import net.caffeinemc.mods.sodium.client.gl.shader.ShaderType;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import qouteall.imm_ptl.core.render.ShaderCodeTransformation;

@Mixin(value = ShaderLoader.class)
public abstract class MixinSodiumShaderLoader {
    
    @WrapOperation(
        method = "loadShader",
        at = @At(
            value = "INVOKE",
            target = "Lnet/caffeinemc/mods/sodium/client/gl/shader/ShaderLoader;getShaderSource(Lnet/minecraft/resources/Identifier;)Ljava/lang/String;",
            remap = true
        ),
        remap = false
    )
    private static String wrapGetShaderSource(
        Identifier name,
        Operation<String> operation,
        @Local(argsOnly = true) ShaderType shaderType
    ) {
        String shaderSource = operation.call(name);
        shaderSource = ShaderCodeTransformation.transform(
            shaderType == ShaderType.VERTEX
                ? com.mojang.blaze3d.shaders.ShaderType.VERTEX
                : com.mojang.blaze3d.shaders.ShaderType.FRAGMENT,
            name.toString(), shaderSource
        );
        return shaderSource;
    }
}
