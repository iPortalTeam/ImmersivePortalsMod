package com.qouteall.immersive_portals.optifine_compatibility;

import net.optifine.Config;

import java.lang.reflect.Field;

public class OptifineCompatibilityHelper {
    private static Class class_Shaders;
    private static Field Shaders_isShaderPackInitialized;
    private static Field Shaders_isShadowPass;
    private static Field Shaders_shaderPack;
    private static boolean originalIsShaderPackInitialized;
    private static boolean isCreatingFakedWorld = false;
    
    public static void init() {
        try {
            class_Shaders = Class.forName("net.optifine.shaders.Shaders");
            Shaders_isShaderPackInitialized = class_Shaders.getDeclaredField(
                "isShaderPackInitialized"
            );
            Shaders_isShaderPackInitialized.setAccessible(true);
            
            Shaders_isShadowPass = class_Shaders.getDeclaredField(
                "isShadowPass"
            );
            Shaders_isShadowPass.setAccessible(true);
            
            Shaders_shaderPack = class_Shaders.getDeclaredField(
                "shaderPack"
            );
            Shaders_shaderPack.setAccessible(true);
        }
        catch (ClassNotFoundException | NoSuchFieldException e) {
            e.printStackTrace();
        }
        
    }
    
    public static boolean getIsUsingShader() {
        return Config.isShaders();
    }
    
    public static boolean getIsShadowPass() {
        try {
            return Shaders_isShadowPass.getBoolean(null);
        }
        catch (IllegalAccessException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public static void onBeginCreatingFakedWorld() {
        isCreatingFakedWorld = true;
    }
    
    public static void onFinishCreatingFakedWorld() {
        isCreatingFakedWorld = false;
    }
    
    public static boolean getIsCreatingFakedWorld() {
        return isCreatingFakedWorld;
    }
}
