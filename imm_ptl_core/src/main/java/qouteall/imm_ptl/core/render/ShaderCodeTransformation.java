package qouteall.imm_ptl.core.render;

import com.google.gson.reflect.TypeToken;
import com.mojang.blaze3d.shaders.Program;
import net.minecraft.resources.ResourceLocation;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.McHelper;
import qouteall.q_misc_util.Helper;

import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

// change the shader code to add clipping mechanism
// 2 ways of clipping:
// 1. vertex shader clipping(requires GL3)
// 2. fragment shader clipping
public class ShaderCodeTransformation {
    public static enum ShaderType {
        vs, fs
    }
    
    private static boolean matches(ShaderType me, Program.Type type) {
        if (type == Program.Type.FRAGMENT) {
            return me == ShaderType.fs;
        }
        else if (type == Program.Type.VERTEX) {
            return me == ShaderType.vs;
        }
        return false;
    }
    
    public static class TransformationEntry {
        public String pattern;
        public List<String> replacement;
    }
    
    public static class Config {
        public ShaderType type;
        public Set<String> affectedShaders;
        public List<TransformationEntry> transformations;
        public boolean debugOutput;
    }
    
    private static final Pattern patternVoidMain = Pattern.compile(
        "void ( )*main( )*\\(( )*( )*\\)( )*(\n)*\\{");
    
    private static List<Config> configs;
    
    public static void init() {
        if (IPGlobal.enableClippingMechanism) {
            String json = McHelper.readTextResource(new ResourceLocation(
                "immersive_portals:shaders/shader_transformation.json"
            ));
            configs = IPGlobal.gson.fromJson(
                json,
                new TypeToken<List<Config>>() {}.getType()
            );
            
            Helper.log("Loaded Shader Code Transformation");
        }
        else {
            Helper.log("Shader Transformation Disabled");
        }
    }
    
    public static String transform(Program.Type type, String shaderId, String inputCode) {
        if (configs == null) {
            Helper.log("Shader Transform Skipping " + shaderId);
            return inputCode;
        }
        
        Config selected = getConfig(type, shaderId);
        
        if (selected == null) {
            return inputCode;
        }
        
        String result = inputCode;
        
        for (TransformationEntry entry : selected.transformations) {
            String replacement = String.join("\n", entry.replacement);
            result = result.replaceAll(entry.pattern, replacement);
        }
        
        if (selected.debugOutput) {
            Helper.log("Shader Transformed " + shaderId + "\n" + result);
        }
        
        return result;
    }
    
    @Nullable
    private static Config getConfig(Program.Type type, String shaderId) {
        return configs.stream().filter(
            config -> matches(config.type, type) &&
                config.affectedShaders.contains(shaderId)
        ).findFirst().orElse(null);
    }
    
    public static boolean shouldAddUniform(String shaderName) {
        if (configs == null) {
            Helper.log("Shader Transform Skipping " + shaderName);
            return false;
        }
        
        return configs.stream().anyMatch(config -> config.affectedShaders.contains(shaderName));
    }
}
