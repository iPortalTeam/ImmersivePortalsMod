package com.qouteall.immersive_portals.optifine_compatibility;

import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.render.PixelCuller;
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
        
        ShaderUniforms shaderUniforms = OFGlobal.getShaderUniforms.get();
        uniform_equationXYZ = shaderUniforms.make3f("cullingEquationXYZ");
        uniform_equationW = shaderUniforms.make1f("cullingEquationW");
    }
    
    public static StringBuilder modifyFragShaderCode(StringBuilder rawCode) {
        if (!cullingEnabled) {
            return rawCode;
        }
        
        if (toReplace == null) {
            throw new RuntimeException("Shader Code Modifier is not initialized");
        }
        
        StringBuilder uniformsDeclarationCode = getUniformsDeclarationCode(rawCode);
        
        Matcher matcher = pattern.matcher(rawCode);
        String result = matcher.replaceFirst(uniformsDeclarationCode + toReplace);
        return new StringBuilder(result);
    }
    
    public static void update() {
        if (PixelCuller.isCullingEnabled) {
            double[] equation = PixelCuller.getActiveCullingPlaneEquation();
            if (equation != null) {
                uniform_equationXYZ.setValue(
                    (float) equation[0],
                    (float) equation[1],
                    (float) equation[2]
                );
                uniform_equationW.setValue((float) equation[3]);
            }
            else {
                Helper.err("Culling Equation is Null?");
                uniform_equationXYZ.setValue(0, 0, 0);
                uniform_equationW.setValue(2333);
            }
        }
        else {
            uniform_equationXYZ.setValue(0, 0, 0);
            uniform_equationW.setValue(2333);
        }
    }
    
    @Deprecated
    public static void loadUniforms() {
//        if (CGlobal.renderer.isRendering()) {
//            double[] equation = PixelCuller.getActiveCullingPlaneEquation();
//            if (equation != null) {
//                uniform_equationXYZ.setValue(
//                    (float) equation[0],
//                    (float) equation[1],
//                    (float) equation[2]
//                );
//                uniform_equationW.setValue((float) equation[3]);
//            }
//            else {
//                uniform_equationXYZ.setValue(0, 0, 0);
//                uniform_equationW.setValue(2333);
//            }
//        }
//        else {
//            uniform_equationXYZ.setValue(0, 0, 0);
//            uniform_equationW.setValue(2333);
//        }
    }
    
    public static boolean shouldModifyShaderCode(Program program) {
        String programName = program.getName();
        return programName.equals("gbuffers_textured") ||
            programName.equals("gbuffers_textured_lit") ||
            programName.equals("gbuffers_water") ||
            programName.equals("gbuffers_terrain") ||
            programName.equals("gbuffers_terrain_solid") ||
            programName.equals("gbuffers_terrain_cutout_mip") ||
            programName.equals("gbuffers_terrain_cutout") ||
            programName.equals("gbuffers_block") ||
            programName.equals("gbuffers_item") ||
            programName.equals("gbuffers_entities") ||
            programName.equals("gbuffers_weather");
    }
    
    //sometimes the shader may not declare gbufferProjectionInverse
    //we have to declare it
    private static StringBuilder getUniformsDeclarationCode(StringBuilder rawCode) {
        StringBuilder uniformsDeclarationCode = new StringBuilder();
        if (rawCode.indexOf("gbufferProjectionInverse") == -1) {
            uniformsDeclarationCode.append("uniform mat4 gbufferProjectionInverse;\n");
        }
        if (rawCode.indexOf("gbufferModelViewInverse") == -1) {
            uniformsDeclarationCode.append("uniform mat4 gbufferModelViewInverse;\n");
        }
        if (rawCode.indexOf("viewWidth") == -1) {
            uniformsDeclarationCode.append("uniform float viewWidth;\n");
        }
        if (rawCode.indexOf("viewHeight") == -1) {
            uniformsDeclarationCode.append("uniform float viewHeight;\n");
        }
        return uniformsDeclarationCode;
    }
}
