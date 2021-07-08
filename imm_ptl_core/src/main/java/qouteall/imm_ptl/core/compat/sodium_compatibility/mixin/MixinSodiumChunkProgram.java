package qouteall.imm_ptl.core.compat.sodium_compatibility.mixin;

import me.jellysquid.mods.sodium.client.gl.GlObject;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.model.vertex.type.ChunkVertexType;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkProgram;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkShaderOptions;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.opengl.GL20C;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.render.FrontClipping;
import qouteall.q_misc_util.Helper;

@Mixin(ChunkProgram.class)
public class MixinSodiumChunkProgram extends GlObject {
    private int uIPClippingEquation;
    
    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInitEnded(RenderDevice owner, int handle, ChunkShaderOptions options, CallbackInfo ci) {
        uIPClippingEquation = GL20C.glGetUniformLocation(this.handle(), "imm_ptl_ClippingEquation");
        if (uIPClippingEquation < 0) {
            Helper.err("uniform imm_ptl_ClippingEquation not found in transformed sodium shader");
            uIPClippingEquation = -1;
        }
    }
    
    /**
     * {@link FrontClipping#updateClippingEquationUniformForCurrentShader()}
     */
    @Inject(method = "setup", at = @At("RETURN"))
    private void onSetup(MatrixStack matrixStack, ChunkVertexType vertexType, CallbackInfo ci) {
        if (uIPClippingEquation != -1) {
            if (FrontClipping.isClippingEnabled) {
                double[] equation = FrontClipping.getActiveClipPlaneEquation();
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
