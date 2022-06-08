package qouteall.imm_ptl.core.mixin.client.render.shader;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.preprocessor.GlslPreprocessor;
import com.mojang.blaze3d.shaders.Program;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import qouteall.imm_ptl.core.render.ShaderCodeTransformation;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

// 800 priority to avoid clash with iris
@Mixin(value = Program.class, priority = 800)
public class MixinProgram {
    /**
     * @author qouteall
     * @reason make the logic clear (Iris redirects it)
     * vanilla copy
     */
    @Overwrite
    public static int compileShaderInternal(
        Program.Type type, String name, InputStream stream,
        String domain, GlslPreprocessor loader
    ) throws IOException {
        String shaderCode = IOUtils.toString(stream, StandardCharsets.UTF_8);
        if (shaderCode == null) {
            throw new IOException("Could not load program " + type.getName());
        }
        
        String transformedShaderCode =
            ShaderCodeTransformation.transform(type, name, shaderCode);
        
        int shaderId = GlStateManager.glCreateShader(type.getGlType());
        
        GlStateManager.glShaderSource(shaderId, loader.process(transformedShaderCode));
        
        GlStateManager.glCompileShader(shaderId);
        
        if (GlStateManager.glGetShaderi(shaderId, 35713) == 0) {
            String errorMessage = StringUtils.trim(
                GlStateManager.glGetShaderInfoLog(shaderId, 32768)
            );
            throw new IOException(
                "Couldn't compile " + type.getName() +
                    " program (" + domain + ", " + name + ") : " + errorMessage
            );
        }
        return shaderId;
    }
}
