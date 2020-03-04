package com.qouteall.immersive_portals.optifine_compatibility;

import com.qouteall.immersive_portals.CHelper;
import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.OFInterface;
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
        if (errorCode != GL_NO_ERROR) {
            String message = "Detected Video Card's Incapability of Depth Format Conversion." +
                "Switch to Compatibility Renderer";
            Helper.err("OpenGL Error" + errorCode);
            Helper.log(message);
            CHelper.printChat(message);
            
            Global.renderMode = Global.RenderMode.compatibility;
        }
    
        OFInterface.bindToShaderFrameBuffer.run();
    }
}
