package qouteall.imm_ptl.core.mixin.client.iris;

import net.coderbot.iris.gl.framebuffer.GlFramebuffer;
import org.lwjgl.opengl.GL30;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Pseudo
@Mixin(GlFramebuffer.class)
public class MixinIrisGlFramebuffer {
//    @ModifyArgs(
//        method = "addDepthAttachment",
//        at = @At(
//            value = "INVOKE",
//            target = "Lorg/lwjgl/opengl/GL30C;glFramebufferTexture2D(IIIII)V"
//        )
//    )
//    public void init2(Args args) {
//        args.set(1, GL30.GL_DEPTH_STENCIL_ATTACHMENT);
//    }
}
