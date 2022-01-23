package qouteall.imm_ptl.core.compat.sodium_compatibility.mixin;

import com.mojang.blaze3d.shaders.Program;
import me.jellysquid.mods.sodium.client.gl.shader.GlShader;
import me.jellysquid.mods.sodium.client.gl.shader.ShaderConstants;
import me.jellysquid.mods.sodium.client.gl.shader.ShaderLoader;
import me.jellysquid.mods.sodium.client.gl.shader.ShaderParser;
import me.jellysquid.mods.sodium.client.gl.shader.ShaderType;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import qouteall.imm_ptl.core.render.ShaderCodeTransformation;

@Mixin(value = ShaderLoader.class, remap = false)
public abstract class MixinSodiumShaderLoader {
    
    @Shadow
    public static String getShaderSource(ResourceLocation name) {
        throw new RuntimeException();
    }
    
    /**
     * @author qouteall
     * @reason hard to inject
     */
    @Overwrite
    public static GlShader loadShader(ShaderType type, ResourceLocation name, ShaderConstants constants) {
        String shaderSource = getShaderSource(name);
        shaderSource = ShaderCodeTransformation.transform(
            type == ShaderType.VERTEX ? Program.Type.VERTEX : Program.Type.FRAGMENT,
            name.toString(), shaderSource
        );
        return new GlShader(type, name, ShaderParser.parseShader(shaderSource, constants));
    }
}
