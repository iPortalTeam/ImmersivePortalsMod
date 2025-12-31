package qouteall.imm_ptl.core.render;

import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.logging.LogUtils;
import me.shedaniel.cloth.clothconfig.shadowed.org.yaml.snakeyaml.Yaml;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.McHelper;
import qouteall.q_misc_util.Helper;

import java.util.List;
import java.util.Set;

public class ShaderCodeTransformation {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    public static enum ShaderStage {
        vs, fs
    }
    
    private static boolean matches(ShaderStage me, ShaderType type) {
        if (type == ShaderType.FRAGMENT) {
            return me == ShaderStage.fs;
        }
        else if (type == ShaderType.VERTEX) {
            return me == ShaderStage.vs;
        }
        return false;
    }
    
    // snakeyaml does not allow passing generic type
    // so use another wrapper type to make list generic type work
    public static class ConfigsObj {
        public List<Config> configs;
    }
    
    public static class TransformationEntry {
        public String comment;
        public String pattern;
        public String replacement;
    }
    
    public static class Config {
        public String comment;
        public ShaderStage type;
        public Set<String> affectedShaders;
        public List<TransformationEntry> transformations;
        public boolean debugOutput;
    }
    
    private static List<Config> configs;
    
    public static void init() {
        if (IPGlobal.enableClippingMechanism) {
            Yaml yaml = new Yaml();
            
            String yamlStr = McHelper.readTextResource(McHelper.newIdentifier(
                "immersive_portals:shaders/shader_transformation.yaml"
            ));
            ConfigsObj configsObj = yaml.loadAs(yamlStr, ConfigsObj.class);
            
            configs = configsObj.configs;
            
            LOGGER.info("Loaded Shader Code Transformation");
        }
        else {
            LOGGER.info("Shader Transformation Disabled");
        }
    }
    
    public static String transform(ShaderType type, Identifier shaderId, String inputCode) {
        if (configs == null) {
            LOGGER.info("Shader Transform Skipping {}", shaderId);
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
            LOGGER.info("Shader Transformed {}\n{}", shaderId, result);
        }
        
        return result;
    }
    
    @Nullable
    private static Config getConfig(ShaderType type, Identifier shaderId) {
        return configs.stream().filter(
            config -> matches(config.type, type) &&
                matchesShaderId(config, shaderId)
        ).findFirst().orElse(null);
    }
    
    public static boolean shouldAddUniform(Identifier shaderId) {
        if (configs == null) {
            LOGGER.info("Shader Transform Skipping {} in shouldAddUniform", shaderId);
            return false;
        }
        
        return configs.stream().anyMatch(
            config -> config.type == ShaderStage.vs && matchesShaderId(config, shaderId)
        );
    }
    
    private static boolean matchesShaderId(Config config, Identifier shaderId) {
        String fullId = shaderId.toString();
        String path = shaderId.getPath();
        String baseName = getBaseName(path);
        String pathNoExt = stripExtension(path);
        String baseNoExt = stripExtension(baseName);
        String fullNoExt = stripExtension(fullId);
        
        for (String configured : config.affectedShaders) {
            String configuredNoExt = stripExtension(configured);
            String configuredPath = configuredNoExt;
            String configuredBase = getBaseName(configuredNoExt);
            
            if (configured.equals(fullId) || configured.equals(path) || configured.equals(baseName)) {
                return true;
            }
            if (configuredNoExt.equals(fullId) || configuredNoExt.equals(path) || configuredNoExt.equals(baseName)) {
                return true;
            }
            if (configuredPath.equals(path) || configuredPath.equals(pathNoExt)) {
                return true;
            }
            if (configuredBase.equals(baseName) || configuredBase.equals(baseNoExt)) {
                return true;
            }
            if (configuredNoExt.equals(fullNoExt)) {
                return true;
            }
        }
        
        return false;
    }
    
    private static String getBaseName(String path) {
        int slash = path.lastIndexOf('/');
        if (slash >= 0 && slash + 1 < path.length()) {
            return path.substring(slash + 1);
        }
        return path;
    }
    
    private static String stripExtension(String value) {
        if (value.endsWith(".vsh") || value.endsWith(".fsh")) {
            return value.substring(0, value.length() - 4);
        }
        return value;
    }
}
