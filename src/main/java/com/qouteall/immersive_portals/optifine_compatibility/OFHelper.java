package com.qouteall.immersive_portals.optifine_compatibility;

import com.qouteall.immersive_portals.CGlobal;
import net.optifine.Config;
import org.lwjgl.opengl.EXTFramebufferObject;

public class OFHelper {
    private static boolean isCreatingFakedWorld = false;
    
    public static void init() {
        
    }
    
    public static boolean getIsUsingShader() {
        if (CGlobal.isOptifinePresent) {
            return Config.isShaders();
        }
        else {
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
    
    public static void bindToShaderFrameBuffer() {
        EXTFramebufferObject.glBindFramebufferEXT(36160, OFGlobal.getDfb.get());
    }
    
}
