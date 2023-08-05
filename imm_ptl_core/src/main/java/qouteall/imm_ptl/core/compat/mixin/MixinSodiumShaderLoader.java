package qouteall.imm_ptl.core.compat.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.shaders.Program;
import me.jellysquid.mods.sodium.client.gl.shader.ShaderLoader;
import me.jellysquid.mods.sodium.client.gl.shader.ShaderType;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import qouteall.imm_ptl.core.render.ShaderCodeTransformation;

@Mixin(value = ShaderLoader.class, remap = false)
public abstract class MixinSodiumShaderLoader {
    
    @WrapOperation(
        method = "loadShader",
        at = @At(
            value = "INVOKE",
            target = "Lme/jellysquid/mods/sodium/client/gl/shader/ShaderLoader;getShaderSource(Lnet/minecraft/resources/ResourceLocation;)Ljava/lang/String;"
        )
    )
    private static String wrapGetShaderSource(
        ResourceLocation name,
        Operation<String> operation,
        @Local ShaderType shaderType
    ) {
        String shaderSource = operation.call(name);
        shaderSource = ShaderCodeTransformation.transform(
            shaderType == ShaderType.VERTEX ? Program.Type.VERTEX : Program.Type.FRAGMENT,
            name.toString(), shaderSource
        );
        return shaderSource;
    }
}
