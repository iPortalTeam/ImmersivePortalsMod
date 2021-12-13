package qouteall.imm_ptl.core.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import qouteall.q_misc_util.Helper;

//it will always be the same size as the main frame buffer
public class SecondaryFrameBuffer {
    public SimpleFramebuffer fb;
    
    public void prepare() {
        Framebuffer mainFrameBuffer = MinecraftClient.getInstance().getFramebuffer();
        int width = mainFrameBuffer.viewportWidth;
        int height = mainFrameBuffer.viewportHeight;
        prepare(width, height);
    }
    
    public void prepare(int width, int height) {
        if (fb == null) {
            fb = new SimpleFramebuffer(
                width, height,
                true,//has depth attachment
                MinecraftClient.IS_SYSTEM_MAC
            );
            fb.checkFramebufferStatus();
            Helper.log("Deferred buffer init");
        }
        if (width != fb.viewportWidth ||
            height != fb.viewportHeight
        ) {
            fb.resize(
                width, height, MinecraftClient.IS_SYSTEM_MAC
            );
            fb.checkFramebufferStatus();
            Helper.log("Deferred buffer resized");
        }
    }
    
    
}
