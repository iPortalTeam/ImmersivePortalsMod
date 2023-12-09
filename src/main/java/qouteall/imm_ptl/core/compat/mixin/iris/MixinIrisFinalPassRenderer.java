package qouteall.imm_ptl.core.compat.mixin.iris;

import net.coderbot.iris.postprocess.FinalPassRenderer;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.IPCGlobal;

import static org.lwjgl.opengl.GL11.GL_STENCIL_TEST;

@Mixin(value = FinalPassRenderer.class, remap = false)
public class MixinIrisFinalPassRenderer {
    @Inject(
        method = "renderFinalPass", at = @At("HEAD")
    )
    void onRenderFinalPass(CallbackInfo ci) {
        if (IPCGlobal.debugEnableStencilWithIris) {
            GL11.glDisable(GL_STENCIL_TEST);
        }
    }
}
