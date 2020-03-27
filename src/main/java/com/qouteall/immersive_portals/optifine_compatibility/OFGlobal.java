package com.qouteall.immersive_portals.optifine_compatibility;

import net.minecraft.world.World;
import net.optifine.shaders.Program;
import net.optifine.shaders.uniform.ShaderUniforms;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class OFGlobal {
    public static RendererDeferred rendererDeferred = new RendererDeferred();
    public static RendererDebugWithShader rendererDebugWithShader = new RendererDebugWithShader();
    public static RendererMixed rendererMixed = new RendererMixed();
    public static ShaderContextManager shaderContextManager = new ShaderContextManager();
    
    public static Consumer<PerDimensionContext> copyContextToObject;
    public static Consumer<PerDimensionContext> copyContextFromObject;
    public static Supplier<Integer> getDfb;
    public static Runnable bindGbuffersTextures;
    public static Supplier<ShaderUniforms> getShaderUniforms;
    public static Supplier<World> getCurrentWorld;
    
    public static boolean alwaysRenderShadowMap = true;
    
    public static Consumer<Program> debugFunc = (p) -> {
    };
    
    public static Runnable bindToShaderFrameBuffer;
    
}
