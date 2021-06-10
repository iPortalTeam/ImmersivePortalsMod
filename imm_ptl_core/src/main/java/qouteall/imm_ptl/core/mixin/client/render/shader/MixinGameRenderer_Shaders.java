package qouteall.imm_ptl.core.mixin.client.render.shader;

import qouteall.imm_ptl.core.render.MyRenderHelper;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Shader;
import net.minecraft.resource.ResourceManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(GameRenderer.class)
public class MixinGameRenderer_Shaders {
    @Shadow
    @Final
    private Map<String, Shader> shaders;
    
    @Inject(
        method = "loadShaders", at = @At("RETURN")
    )
    private void onLoadShaders(ResourceManager manager, CallbackInfo ci) {
        MyRenderHelper.loadShaderSignal.emit(
            manager, (shader) -> {
                shaders.put(shader.getName(), shader);
            }
        );
    }
}
