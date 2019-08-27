package com.qouteall.immersive_portals.optifine_compatibility;

import com.qouteall.immersive_portals.CGlobal;
import net.minecraft.client.MinecraftClient;
import net.minecraft.world.dimension.DimensionType;
import net.optifine.Config;
import net.optifine.shaders.Shaders;
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
        Class<Shaders> loadTheClass = Shaders.class;
        EXTFramebufferObject.glBindFramebufferEXT(36160, OFGlobal.getDfb.get());
    }
    
    public static void onShaderInit(MinecraftClient mc, DimensionType currDimension) {
//        if (OFGlobal.doForceInitSequence) {
//            if (!OFGlobal.shaderContextManager.isContextSwitched()) {
//                if (currDimension != DimensionType.OVERWORLD) {
//                    ClientWorld overWorld = CGlobal.clientWorldLoader.getOrCreateFakedWorld(
//                        DimensionType.OVERWORLD
//                    );
//                    ClientWorld originalWorld = mc.world;
//                    mc.world = overWorld;
//                    OFGlobal.shaderContextManager.switchContextAndRun(Shaders::init);
//                    mc.world = originalWorld;
//                }
//            }
//        }
    }
}
