package qouteall.imm_ptl.core.compat.sodium_compatibility.mixin;

import me.jellysquid.mods.sodium.client.gl.GlObject;
import me.jellysquid.mods.sodium.client.model.vertex.type.ChunkVertexType;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkShaderOptions;
import net.coderbot.iris.gl.program.ProgramSamplers;
import net.coderbot.iris.gl.program.ProgramUniforms;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL20C;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.render.FrontClipping;
import qouteall.q_misc_util.Helper;

@Pseudo
@Mixin(targets = "me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkShaderInterface")
public class MixinIrisSodiumChunkShaderInterface {
    private int uIPClippingEquation;
    
    private void ip_init(int shaderId) {
        uIPClippingEquation = GL20C.glGetUniformLocation(shaderId, "imm_ptl_ClippingEquation");
        if (uIPClippingEquation < 0) {
            Helper.err("uniform imm_ptl_ClippingEquation not found in transformed sodium shader");
            uIPClippingEquation = -1;
        }
    }
    
    @Inject(
        method = "<init>",
        at = @At("RETURN"),
        require = 0
    )
    private void onInitIrisSodium(
        @Coerce Object context,
        @Coerce Object options,
        @Coerce Object irisProgramUniforms,
        @Coerce Object irisProgramSamplers,
        CallbackInfo ci
    ) {
        ip_init(((GlObject) context).handle());
    }
    
    @Inject(
        method = "setup",
        at = @At("RETURN")
    )
    private void onSetup(ChunkVertexType vertexType, CallbackInfo ci) {
        if (uIPClippingEquation != -1) {
            if (FrontClipping.isClippingEnabled) {
                double[] equation = FrontClipping.getActiveClipPlaneEquationForEntities();
                GL20C.glUniform4f(
                    uIPClippingEquation,
                    (float) equation[0],
                    (float) equation[1],
                    (float) equation[2],
                    (float) equation[3]
                );
            }
            else {
                GL20C.glUniform4f(
                    uIPClippingEquation,
                    0, 0, 0, 1
                );
            }
        }
    }
}
