package com.qouteall.immersive_portals;

import java.lang.reflect.Field;

public class OptifineCompatibilityHelper {
    private static Class class_Shaders;
    private static Field Shaders_isShaderPackInitialized;
    private static Field Shaders_isShadowPass;
    private static Field Shaders_shaderPack;
    private static boolean originalIsShaderPackInitialized;
    
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
        try {
            return Shaders_shaderPack.get(null) != null;
        }
        catch (IllegalAccessException e) {
            e.printStackTrace();
            return false;
        }
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
        if (CGlobal.isOptifinePresent) {
            try {
                originalIsShaderPackInitialized = Shaders_isShaderPackInitialized.getBoolean(null);
                Shaders_isShaderPackInitialized.setBoolean(null, false);
            }
            catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
    }
    
    public static void onFinishCreatingFakedWorld() {
        if (CGlobal.isOptifinePresent) {
            try {
                Shaders_isShaderPackInitialized.setBoolean(null, originalIsShaderPackInitialized);
            }
            catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
