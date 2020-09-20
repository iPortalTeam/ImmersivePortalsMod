package com.qouteall.immersive_portals.optifine_compatibility.mixin_optifine;

import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.optifine_compatibility.OFGlobal;
import com.qouteall.immersive_portals.optifine_compatibility.ShaderClippingManager;
import com.qouteall.immersive_portals.render.context_management.PortalRendering;
import com.qouteall.immersive_portals.render.context_management.RenderStates;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.optifine.shaders.IShaderPack;
import net.optifine.shaders.Program;
import net.optifine.shaders.Shaders;
import net.optifine.shaders.uniform.CustomUniforms;
import net.optifine.shaders.uniform.ShaderUniforms;
import org.lwjgl.BufferUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

@Mixin(value = Shaders.class, remap = false)
public abstract class MixinShaders {
    @Shadow
    private static double previousCameraPositionX;
    
    @Shadow
    private static double previousCameraPositionY;
    
    @Shadow
    private static double previousCameraPositionZ;
    
    @Shadow
    private static double cameraPositionX;
    
    @Shadow
    private static double cameraPositionY;
    
    @Shadow
    private static double cameraPositionZ;
    
    @Shadow
    private static ClientWorld currentWorld;
    
    @Shadow
    private static void init() {
        throw new RuntimeException();
    }
    
    @Shadow
    private static IShaderPack shaderPack;
    
    @Shadow
    @Final
    private static ByteBuffer bigBuffer;
    
    //avoid uninit when creating faked world
    @Inject(method = "checkWorldChanged", at = @At("HEAD"), cancellable = true)
    private static void onCheckWorldChanged(ClientWorld world, CallbackInfo ci) {
        if (CGlobal.clientWorldLoader.getIsCreatingClientWorld()) {
            ci.cancel();
        }
    }
    
    //if the main shader context uninits, uninit all shader context
    @Inject(
        method = "uninit",
        at = @At(
            value = "INVOKE_STRING",
            target = "Lnet/optifine/shaders/SMCLog;info(Ljava/lang/String;)V",
            args = "ldc=Uninit"
        )
    )
    private static void onUninit(CallbackInfo ci) {
        OFGlobal.shaderContextManager.onShaderUninit();
    }
    
    @Inject(method = "storeConfig", at = @At("HEAD"))
    private static void onStoreConfig(CallbackInfo ci) {
        if (OFGlobal.shaderContextManager.isContextSwitched()) {
            Helper.err("Trying to store config when context switched");
            ci.cancel();
        }
    }
    
    
    //loading shader pack will change vertex format
    //avoid changing vertex format when rebuilding
    @Inject(method = "loadShaderPack", at = @At("HEAD"))
    private static void onAboutToLoadShaderPack(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.worldRenderer != null) {
            client.worldRenderer.reload();
        }
    }
    
    //do not eat error
    @Redirect(
        method = "beginRender",
        at = @At(
            value = "INVOKE",
            target = "Lnet/optifine/shaders/Shaders;init()V"
        )
    )
    private static void redirectInitOnBeginRender() {
        try {
            init();
        }
        catch (Throwable e) {
            e.printStackTrace();
            throw e;
        }
    }
    
    @Inject(method = "loadShaderPack", at = @At("TAIL"))
    private static void onShaderPackLoaded(CallbackInfo ci) {
        OFGlobal.shaderContextManager.updateTemplateContext();
    }
    
    @Redirect(
        method = "init",
        at = @At(
            value = "INVOKE",
            target = "Lnet/optifine/shaders/uniform/ShaderUniforms;reset()V"
        )
    )
    private static void redirectShaderUniformReset(ShaderUniforms shaderUniforms) {
        if (!OFGlobal.shaderContextManager.isContextSwitched()) {
            shaderUniforms.reset();
        }
    }
    
    //multiple context share the same custom uniforms
    //do not reset when initializing the second context
    @Redirect(
        method = "init",
        at = @At(
            value = "INVOKE",
            target = "Lnet/optifine/shaders/uniform/CustomUniforms;reset()V"
        )
    )
    private static void redirectCustomUniformReset(CustomUniforms customUniforms) {
        if (!OFGlobal.shaderContextManager.isContextSwitched()) {
            customUniforms.reset();
        }
    }
    
    private static boolean shouldModifyShaderCode;
    
    @Inject(method = "createFragShader", at = @At("HEAD"))
    private static void onCreateFragShader(
        Program program,
        String filename,
        CallbackInfoReturnable<Integer> cir
    ) {
        shouldModifyShaderCode = ShaderClippingManager.shouldModifyShaderCode(program);
    }
    
    @ModifyVariable(
        method = "createFragShader",
        at = @At(
            value = "FIELD",
            target = "Lnet/optifine/shaders/Shaders;saveFinalShaders:Z"
        )
    )
    private static StringBuilder modifyFragShaderCode(StringBuilder shaderCode) {
        if (!shouldModifyShaderCode) {
            return shaderCode;
        }
        return ShaderClippingManager.modifyFragShaderCode(shaderCode);
    }
    
    @Inject(
        method = "useProgram",
        at = @At("TAIL")
    )
    private static void onLoadingUniforms(Program program, CallbackInfo ci) {
        if (ShaderClippingManager.shouldModifyShaderCode(program)) {
            ShaderClippingManager.loadUniforms();
        }
        OFGlobal.debugFunc.accept(program);
    }
    
    //in setCameraShadow() it will set some uniforms
    //but it's illegal to set a uniform without binding program
    @Inject(method = "setCameraShadow", at = @At("HEAD"))
    private static void onSetCameraShadow(
        MatrixStack matrixStack,
        Camera activeRenderInfo,
        float partialTicks,
        CallbackInfo ci
    ) {
        Shaders.useProgram(Shaders.ProgramShadow);
    }
    
    //sometimes the bigger buffer's space is not enough
    //I don't know why
    //so I allocate new buffers
    @Inject(method = "nextIntBuffer", at = @At("HEAD"), cancellable = true)
    private static void onNextIntBuffer(int size, CallbackInfoReturnable<IntBuffer> cir) {
        ByteBuffer buffer = bigBuffer;
        int pos = buffer.limit();
        if (buffer.capacity() <= pos + size * 4) {
            //big buffer is not big enough
            cir.setReturnValue(BufferUtils.createIntBuffer(size));
            cir.cancel();
        }
    }
    
    //avoid too bright when rendering remote world
    @Redirect(
        method = "beginRender",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/entity/EntityRenderDispatcher;getLight(Lnet/minecraft/entity/Entity;F)I",
            remap = true
        )
    )
    private static int redirectGetLight(
        EntityRenderDispatcher entityRenderDispatcher,
        Entity entity,
        float tickDelta
    ) {
        if (PortalRendering.isRendering()) {
            return RenderStates.originalCameraLightPacked;
        }
        else {
            return entityRenderDispatcher.getLight(entity, tickDelta);
        }
    }
    
//    @Redirect(
//        method = "init",
//        at = @At(
//            value = "INVOKE",
//            target = "Lnet/minecraft/world/World;getRegistryKey()Lnet/minecraft/util/registry/RegistryKey;",
//            remap = true
//        ),
//        remap = false
//    )
//    private static RegistryKey<World> redirectGetRegistryKey(World world){
//        return RenderDimensionRedirect.getRedirectedDimension(world.getRegistryKey());
//    }
    
    //correct the previous camera pos
//    @Inject(method = "beginRender", at = @At("TAIL"))
//    private static void onBeginRender(
//        MinecraftClient minecraft,
//        Camera activeRenderInfo,
//        float partialTicks,
//        long finishTimeNano,
//        CallbackInfo ci
//    ) {
//        previousCameraPositionX = cameraPositionX - MyRenderHelper.cameraPosDelta.x;
//        previousCameraPositionY = cameraPositionY - MyRenderHelper.cameraPosDelta.y;
//        previousCameraPositionZ = cameraPositionZ - MyRenderHelper.cameraPosDelta.z;
//    }
    
    
}
