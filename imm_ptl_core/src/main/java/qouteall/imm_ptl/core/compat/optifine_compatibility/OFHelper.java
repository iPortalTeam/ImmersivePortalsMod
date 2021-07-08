package qouteall.imm_ptl.core.compat.optifine_compatibility;

import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.q_misc_util.Helper;
import net.minecraft.client.gl.Framebuffer;
import net.optifine.shaders.Shaders;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL11.GL_NO_ERROR;

public class OFHelper {
    
    
    public static void copyFromShaderFbTo(Framebuffer destFb, int copyComponent) {
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, OFGlobal.getDfb.get());
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, destFb.fbo);
        
        GL30.glBlitFramebuffer(
            0, 0, Shaders.renderWidth, Shaders.renderHeight,
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
        
        OFGlobal.bindToShaderFrameBuffer.run();
    }
    
    public static boolean isChocapicShader() {
        String name = OFGlobal.getCurrentShaderpack.get().getName();
        return name.toLowerCase().indexOf("chocapic") != -1;
    }
}
