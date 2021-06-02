package com.qouteall.immersive_portals.render;

import com.google.gson.reflect.TypeToken;
import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import net.minecraft.client.gl.Program;
import net.minecraft.util.Identifier;
import org.apache.commons.lang3.Validate;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

// change the shader code to add clipping mechanism
// 2 ways of clipping:
// 1. vertex shader clipping(requires GL3)
// 2. fragment shader clipping
public class ShaderCodeTransformation {
    public static enum Type {
        vs, fs
    }
    
    public static class Config {
        public Type type;
        public Set<String> affectedShaders;
        public String pattern;
        public List<String> replacement;
        public boolean debugOutput;
    }
    
    private static final Pattern patternVoidMain = Pattern.compile(
        "void ( )*main( )*\\(( )*( )*\\)( )*(\n)*\\{");
    
    private static List<Config> configs;
    
    public static void init() {
        String json = McHelper.readTextResource(new Identifier(
            "immersive_portals:shaders/shader_transformation.json"
        ));
        configs = Global.gson.fromJson(
            json,
            new TypeToken<List<Config>>() {}.getType()
        );
        
        Helper.log("Loaded Shader Code Transformation");
    }
    
    public static String transform(Program.Type type, String shaderId, String inputCode) {
        Validate.notNull(configs);
        
        Config selected = configs.stream().filter(
            config -> config.type == Type.vs &&
                config.affectedShaders.contains(shaderId)
        ).findFirst().orElse(null);
        
        if (selected == null) {
            return inputCode;
        }
        
        String replacement = selected.replacement.stream().collect(Collectors.joining("\n"));
        String result = inputCode.replaceFirst(selected.pattern, replacement);
        
        return result;
    }
}
