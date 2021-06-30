package qouteall.imm_ptl.core.mixin.client.iris;

import net.coderbot.iris.rendertarget.DepthTexture;
import org.lwjgl.opengl.GL30;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Pseudo
@Mixin(DepthTexture.class)
public class MixinIrisDepthTexture {
//    @ModifyArgs(
//        method = "resize",
//        at = @At(
//            value = "INVOKE",
//            target = "Lorg/lwjgl/opengl/GL11C;glTexImage2D(IIIIIIIILjava/nio/ByteBuffer;)V"
//        )
//    )
//    public void init(Args args) {
//        args.set(2, GL30.GL_DEPTH24_STENCIL8);
//        args.set(6, GL30.GL_DEPTH_STENCIL);
//        args.set(7, GL30.GL_UNSIGNED_INT_24_8);
//
////        args.set(2, GL30.GL_DEPTH32F_STENCIL8);
////        args.set(6, GL30.GL_DEPTH_STENCIL);
////        args.set(7, GL30.GL_FLOAT_32_UNSIGNED_INT_24_8_REV);
//    }
}
