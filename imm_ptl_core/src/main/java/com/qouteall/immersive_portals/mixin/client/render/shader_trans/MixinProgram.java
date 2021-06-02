package com.qouteall.immersive_portals.mixin.client.render.shader_trans;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.render.ShaderCodeTransformation;
import net.minecraft.client.gl.GLImportProcessor;
import net.minecraft.client.gl.Program;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.IOException;
import java.io.InputStream;

@Mixin(value = Program.class, priority = 1200)
public class MixinProgram {
    /**
     * @author qouteall
     */
    @Overwrite
    protected static int loadProgram(
        Program.Type type, String name, InputStream stream,
        String domain, GLImportProcessor loader
    ) throws IOException {
        String shaderCode = TextureUtil.readResourceAsString(stream);
        if (shaderCode == null) {
            throw new IOException("Could not load program " + type.getName());
        }
        
        String transformedShaderCode =
            ShaderCodeTransformation.transform(type, name, shaderCode);
        
        Helper.log("Shader code transformed:\n" + transformedShaderCode);
        
        int shaderId = GlStateManager.glCreateShader(type.getGlType());
        
        GlStateManager.glShaderSource(shaderId, loader.readSource(transformedShaderCode));
        
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
