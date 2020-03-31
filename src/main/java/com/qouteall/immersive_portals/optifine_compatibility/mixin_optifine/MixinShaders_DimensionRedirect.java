package com.qouteall.immersive_portals.optifine_compatibility.mixin_optifine;

import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.render.context_management.RenderDimensionRedirect;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.dimension.DimensionType;
import net.optifine.shaders.IShaderPack;
import net.optifine.shaders.ShaderPackDefault;
import net.optifine.shaders.Shaders;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Shaders.class, remap = false)
public class MixinShaders_DimensionRedirect {
    
    @Shadow
    private static ClientWorld currentWorld;
    
    @Shadow
    private static IShaderPack shaderPack;
    
    @Inject(method = "init", at = @At("HEAD"))
    private static void onInit(CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        DimensionType currDimension = mc.world.dimension.getType();
        
        Helper.log("Shader init " + currDimension);
        
        if (RenderDimensionRedirect.isNoShader(currentWorld.dimension.getType())) {
            shaderPack = new ShaderPackDefault();
            Helper.log("Set to internal shader");
        }
    }
    
    @Redirect(
        method = "init",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/dimension/DimensionType;getRawId()I"
        )
    )
    private static int redirectGetDimensionRawId(DimensionType dimensionType) {
        return RenderDimensionRedirect.getRedirectedDimension(dimensionType).getRawId();
    }
    
    //redirect dimension for shadow camera
    @Redirect(
        method = "Lnet/optifine/shaders/Shaders;setCameraShadow(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/Camera;F)V",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/MinecraftClient;world:Lnet/minecraft/client/world/ClientWorld;"
        )
    )
    private static ClientWorld redirectWorldForShadowCamera(MinecraftClient client) {
        return CGlobal.clientWorldLoader.getWorld(
            RenderDimensionRedirect.getRedirectedDimension(
                client.world.getDimension().getType()
            )
        );
    }
    
    @Redirect(
        method = "beginRender",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/MinecraftClient;world:Lnet/minecraft/client/world/ClientWorld;",
            ordinal = 1
        )
    )
    private static ClientWorld redirectWorldInBeginRender(MinecraftClient client) {
        return CGlobal.clientWorldLoader.getWorld(
            RenderDimensionRedirect.getRedirectedDimension(
                client.world.getDimension().getType()
            )
        );
    }
}
