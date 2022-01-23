package qouteall.imm_ptl.core.mixin.client.render;

import com.mojang.blaze3d.shaders.Program;
import net.minecraft.client.renderer.ShaderInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import qouteall.imm_ptl.core.ClientWorldLoader;

import java.util.HashMap;
import java.util.Map;

@Mixin(ShaderInstance.class)
public class MixinShaderForIris {
    // if iris is present, avoid reusing other dimensions' program in cache
    @Redirect(
        method = "Lnet/minecraft/client/renderer/ShaderInstance;getOrCreate(Lnet/minecraft/server/packs/resources/ResourceProvider;Lcom/mojang/blaze3d/shaders/Program$Type;Ljava/lang/String;)Lcom/mojang/blaze3d/shaders/Program;",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/shaders/Program$Type;getPrograms()Ljava/util/Map;"
        )
    )
    private static Map redirectGetProgramCache(Program.Type type) {
        if (ClientWorldLoader.getIsInitialized()) {
            return new HashMap();
        }
        return type.getPrograms();
    }
}
