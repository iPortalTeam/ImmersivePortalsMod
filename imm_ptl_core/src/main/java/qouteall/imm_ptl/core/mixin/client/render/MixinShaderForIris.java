package qouteall.imm_ptl.core.mixin.client.render;

import net.coderbot.iris.Iris;
import net.coderbot.iris.shaderpack.DimensionId;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Program;
import net.minecraft.client.render.Shader;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import qouteall.imm_ptl.core.ClientWorldLoader;

import java.util.HashMap;
import java.util.Map;

@Mixin(Shader.class)
public class MixinShaderForIris {
    // if iris is present, avoid reusing other dimensions' program in cache
    @Redirect(
        method = "loadProgram",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gl/Program$Type;getProgramCache()Ljava/util/Map;"
        )
    )
    private static Map redirectGetProgramCache(Program.Type type) {
        if (ClientWorldLoader.getIsInitialized()) {
            return new HashMap();
        }
        return type.getProgramCache();
    }
}
