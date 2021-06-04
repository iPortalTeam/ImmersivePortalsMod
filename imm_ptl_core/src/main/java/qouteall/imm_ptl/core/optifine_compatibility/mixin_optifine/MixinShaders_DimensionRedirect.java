package qouteall.imm_ptl.core.optifine_compatibility.mixin_optifine;

import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.imm_ptl.core.Helper;
import qouteall.imm_ptl.core.render.context_management.RenderDimensionRedirect;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import net.optifine.shaders.IShaderPack;
import net.optifine.shaders.ShaderPackDefault;
import net.optifine.shaders.Shaders;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(value = Shaders.class)
public class MixinShaders_DimensionRedirect {
    
    @Shadow(remap = false)
    private static ClientWorld currentWorld;
    
    @Shadow(remap = false)
    private static IShaderPack shaderPack;
    
    @Inject(method = "init", at = @At("HEAD"), remap = false)
    private static void onInit(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        RegistryKey<World> currDimension = client.world.getRegistryKey();
        
        Helper.log("Shader init " + currDimension);
        
        if (RenderDimensionRedirect.isNoShader(currentWorld.getRegistryKey())) {
            shaderPack = new ShaderPackDefault();
            Helper.log("Set to internal shader");
        }
    }
    
    //redirect dimension for shadow camera
    @Redirect(
        method = "setCameraShadow",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/MinecraftClient;world:Lnet/minecraft/client/world/ClientWorld;",
            remap = true
        ),
        remap = false
    )
    private static ClientWorld redirectWorldForShadowCamera(MinecraftClient client) {
        return ClientWorldLoader.getWorld(RenderDimensionRedirect.getRedirectedDimension(
                client.world.getRegistryKey()
            ));
    }
    
    @Redirect(
        method = "beginRender",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/MinecraftClient;world:Lnet/minecraft/client/world/ClientWorld;",
            ordinal = 1,
            remap = true
        ),
        remap = false
    )
    private static ClientWorld redirectWorldInBeginRender(MinecraftClient client) {
        return ClientWorldLoader.getWorld(RenderDimensionRedirect.getRedirectedDimension(
                client.world.getRegistryKey()
            ));
    }
    
}
