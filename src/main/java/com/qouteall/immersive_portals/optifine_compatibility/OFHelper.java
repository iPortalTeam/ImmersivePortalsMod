package com.qouteall.immersive_portals.optifine_compatibility;

import com.qouteall.immersive_portals.OFInterface;
import net.minecraft.client.gl.GlFramebuffer;
import net.optifine.shaders.Shaders;
import org.lwjgl.opengl.GL30;

import static org.lwjgl.opengl.GL11.GL_NEAREST;

public class OFHelper {
    
    
    public static void copyFromShaderFbTo(GlFramebuffer destFb, int copyComponent) {
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, OFGlobal.getDfb.get());
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, destFb.fbo);
        
        GL30.glBlitFramebuffer(
            0, 0, Shaders.renderWidth, Shaders.renderHeight,
            0, 0, destFb.viewWidth, destFb.viewHeight,
            copyComponent, GL_NEAREST
        );
        
        OFInterface.bindToShaderFrameBuffer.run();
    }
}
