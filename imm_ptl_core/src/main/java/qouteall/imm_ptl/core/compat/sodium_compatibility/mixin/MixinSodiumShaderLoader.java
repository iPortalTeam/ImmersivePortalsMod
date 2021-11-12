package qouteall.imm_ptl.core.compat.sodium_compatibility.mixin;

import me.jellysquid.mods.sodium.client.gl.shader.GlShader;
import me.jellysquid.mods.sodium.client.gl.shader.ShaderConstants;
import me.jellysquid.mods.sodium.client.gl.shader.ShaderLoader;
import me.jellysquid.mods.sodium.client.gl.shader.ShaderParser;
import me.jellysquid.mods.sodium.client.gl.shader.ShaderType;
import net.minecraft.client.gl.Program;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import qouteall.imm_ptl.core.render.ShaderCodeTransformation;

@Mixin(value = ShaderLoader.class, remap = false)
public abstract class MixinSodiumShaderLoader {
    
    @Shadow
    public static String getShaderSource(Identifier name) {
        throw new RuntimeException();
    }
    
    // TODO recover
    @Inject(
        method = "loadShader",
        at = @At("RETURN"),
        cancellable = true
    )
    private static void onLoadShader(
        ShaderType type, Identifier name,
        ShaderConstants constants, CallbackInfoReturnable<GlShader> cir
    ) {
    }
    
//    /**
//     * @author qouteall
//     * @reason hard to inject
//     */
//    @Overwrite
//    public static GlShader loadShader(ShaderType type, Identifier name, ShaderConstants constants) {
//        String shaderSource = getShaderSource(name);
//        shaderSource = ShaderCodeTransformation.transform(
//            type == ShaderType.VERTEX ? Program.Type.VERTEX : Program.Type.FRAGMENT,
//            name.toString(), shaderSource
//        );
//        return new GlShader(type, name, ShaderParser.parseShader(shaderSource, constants));
//    }
}
