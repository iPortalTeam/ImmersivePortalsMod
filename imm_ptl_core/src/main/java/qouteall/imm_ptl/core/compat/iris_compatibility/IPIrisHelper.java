package qouteall.imm_ptl.core.compat.iris_compatibility;

public class IPIrisHelper {

//    // may have issue on amd
//    public static void copyFromIrisShaderFbTo(Framebuffer destFb, int copyComponent) {
//        GlFramebuffer baselineFramebuffer = getIrisBaselineFramebuffer();
//        baselineFramebuffer.bindAsReadBuffer();
//
//        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, destFb.fbo);
//
//        GL30.glBlitFramebuffer(
//            0, 0, destFb.textureWidth, destFb.textureHeight,
//            0, 0, destFb.textureWidth, destFb.textureHeight,
//            copyComponent, GL_NEAREST
//        );
//
//        int errorCode = GL11.glGetError();
//        if (errorCode != GL_NO_ERROR && IPGlobal.renderMode == IPGlobal.RenderMode.normal) {
//            String message = "[Immersive Portals] Switch to Compatibility Portal Renderer";
//            Helper.err("OpenGL Error" + errorCode);
//            Helper.log(message);
//            CHelper.printChat(message);
//
//            IPGlobal.renderMode = IPGlobal.RenderMode.compatibility;
//        }
//
//        getIrisBaselineFramebuffer().bind();
//    }
//
//    static GlFramebuffer getIrisBaselineFramebuffer() {
//        WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipeline();
//        NewWorldRenderingPipeline newPipeline = (NewWorldRenderingPipeline) pipeline;
//
//        GlFramebuffer baselineFramebuffer = newPipeline.getBaselineFramebuffer();
//        return baselineFramebuffer;
//    }
//
//    @Deprecated
//    private static void copyDepth(
//        GlFramebuffer from,
//        int toTexture,
//        int width, int height
//    ) {
//        doCopyComponent(from, toTexture, width, height, GL20C.GL_DEPTH_COMPONENT);
//    }
//
//    @Deprecated
//    private static void doCopyComponent(
//        GlFramebuffer from, int toTexture, int width, int height, int copiedComponent
//    ) {
//        from.bindAsReadBuffer();
//        GlStateManager._bindTexture(toTexture);
//        GL20C.glCopyTexImage2D(GL20C.GL_TEXTURE_2D, 0, copiedComponent, 0, 0, width, height, 0);
//        GlStateManager._bindTexture(0);
//    }
}
