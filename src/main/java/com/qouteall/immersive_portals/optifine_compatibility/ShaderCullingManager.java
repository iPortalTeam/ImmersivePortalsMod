package com.qouteall.immersive_portals.optifine_compatibility;

import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.my_util.Helper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import net.optifine.shaders.Program;
import net.optifine.shaders.uniform.ShaderUniform1f;
import net.optifine.shaders.uniform.ShaderUniform3f;
import net.optifine.shaders.uniform.ShaderUniforms;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//glClipPlane is not compatible with shaders
//so I have to modify shader code
public class ShaderCullingManager {
    
    private static final Pattern pattern = Pattern.compile(
        "void ( )*main( )*\\(( )*( )*\\)( )*(\n)*\\{");
    
    private static String toReplace;
    
    private static final Identifier transformation = new Identifier(
        "immersive_portals:shaders/shader_code_transformation.txt"
    );
    
    public static ShaderUniform3f uniform_equationXYZ;
    public static ShaderUniform1f uniform_equationW;
    
    public static boolean cullingEnabled = true;
    
    public static void init() {
        try {
            InputStream inputStream =
                MinecraftClient.getInstance().getResourceManager().getResource(
                    transformation
                ).getInputStream();
            
            toReplace = IOUtils.toString(inputStream, Charset.defaultCharset());
        }
        catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
        
        Helper.log("loaded shader code replacement\n" + toReplace);
        
        ShaderUniforms shaderUniforms = OFGlobal.getShaderUniforms.get();
        uniform_equationXYZ = shaderUniforms.make3f("cullingEquationXYZ");
        uniform_equationW = shaderUniforms.make1f("cullingEquationW");
    }
    
    public static StringBuilder modifyFragShaderCode(StringBuilder stringBuilder) {
        if (!cullingEnabled) {
            return stringBuilder;
        }
        
        if (toReplace == null) {
            throw new IllegalStateException("Shader Code Modifier is not initialized");
        }
        
        Matcher matcher = pattern.matcher(stringBuilder);
        String result = matcher.replaceFirst(toReplace);
        return new StringBuilder(result);
    }
    
    public static void loadUniforms() {
        if (CGlobal.renderer.isRendering()) {
            double[] equation = CGlobal.myGameRenderer.getClipPlaneEquation();
            uniform_equationXYZ.setValue(
                (float) equation[0],
                (float) equation[1],
                (float) equation[2]
            );
            uniform_equationW.setValue((float) equation[3]);
        }
        else {
            uniform_equationXYZ.setValue(0, 0, 0);
            uniform_equationW.setValue(2333);
        }
    }
    
    public static boolean getShouldModifyShaderCode(Program program) {
        return program.getName().equals("gbuffers_terrain") ||
            program.getName().equals("gbuffers_terrain_solid") ||
            program.getName().equals("gbuffers_terrain_cutout_mip") ||
            program.getName().equals("gbuffers_terrain_cutout");
    }
}
