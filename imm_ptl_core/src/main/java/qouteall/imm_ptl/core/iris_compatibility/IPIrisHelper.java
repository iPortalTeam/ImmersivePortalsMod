package qouteall.imm_ptl.core.iris_compatibility;

import com.mojang.blaze3d.platform.GlStateManager;
import net.coderbot.iris.Iris;
import net.coderbot.iris.gl.framebuffer.GlFramebuffer;
import net.coderbot.iris.pipeline.WorldRenderingPipeline;
import net.coderbot.iris.pipeline.newshader.NewWorldRenderingPipeline;
import net.minecraft.client.gl.Framebuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.q_misc_util.Helper;

import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL11.GL_NO_ERROR;

public class IPIrisHelper {
    
    // may have issue on amd
    public static void copyFromIrisShaderFbTo(Framebuffer destFb, int copyComponent) {
        GlFramebuffer baselineFramebuffer = getIrisBaselineFramebuffer();
        baselineFramebuffer.bindAsReadBuffer();
        
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, destFb.fbo);
        
        GL30.glBlitFramebuffer(
            0, 0, destFb.textureWidth, destFb.textureHeight,
            0, 0, destFb.textureWidth, destFb.textureHeight,
            copyComponent, GL_NEAREST
        );
        
        int errorCode = GL11.glGetError();
        if (errorCode != GL_NO_ERROR && IPGlobal.renderMode == IPGlobal.RenderMode.normal) {
            String message = "[Immersive Portals] Switch to Compatibility Portal Renderer";
            Helper.err("OpenGL Error" + errorCode);
            Helper.log(message);
            CHelper.printChat(message);
            
            IPGlobal.renderMode = IPGlobal.RenderMode.compatibility;
        }
        
        getIrisBaselineFramebuffer().bind();
    }
    
    static GlFramebuffer getIrisBaselineFramebuffer() {
        WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipeline();
        NewWorldRenderingPipeline newPipeline = (NewWorldRenderingPipeline) pipeline;
        
        GlFramebuffer baselineFramebuffer = newPipeline.getBaselineFramebuffer();
        return baselineFramebuffer;
    }
    
    @Deprecated
    private static void copyDepth(
        GlFramebuffer from,
        int toTexture,
        int width, int height
    ) {
        doCopyComponent(from, toTexture, width, height, GL20C.GL_DEPTH_COMPONENT);
    }
    
    @Deprecated
    private static void doCopyComponent(
        GlFramebuffer from, int toTexture, int width, int height, int copiedComponent
    ) {
        from.bindAsReadBuffer();
        GlStateManager._bindTexture(toTexture);
        GL20C.glCopyTexImage2D(GL20C.GL_TEXTURE_2D, 0, copiedComponent, 0, 0, width, height, 0);
        GlStateManager._bindTexture(0);
    }
}
