package com.qouteall.immersive_portals;

import net.fabricmc.loader.FabricLoader;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class SatinCompatibility {
    private static boolean isSatinPresent = false;
    private static Object readableDepthFramebuffers;
    private static Method methodIsActive;
    
    public static void init() {
        isSatinPresent = FabricLoader.INSTANCE.isModLoaded("satin");
        
        if (isSatinPresent) {
            try {
                Class<?> classSatinFeatures = Class.forName("ladysnake.satin.config.SatinFeatures");
                Method methodGetInstance = classSatinFeatures.getMethod("getInstance");
                Field fieldReadableDepthFramebuffers =
                    classSatinFeatures.getDeclaredField("readableDepthFramebuffers");
                Object instance = methodGetInstance.invoke(null);
                readableDepthFramebuffers = fieldReadableDepthFramebuffers.get(instance);
                methodIsActive = readableDepthFramebuffers.getClass().getMethod("isActive");
            }
            catch (Throwable e) {
                throw new IllegalStateException(e);
            }
            
        }
    }
    
    public static boolean isSatinShaderEnabled() {
        if (!isSatinPresent) {
            return false;
        }
        
        try {
            return (Boolean) methodIsActive.invoke(readableDepthFramebuffers);
        }
        catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            return false;
        }
    }
}
